const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { auth } = require("firebase-functions/v1");
const admin = require("firebase-admin");

if (admin.apps.length === 0) {
    admin.initializeApp();
}

// --- FUNÇÃO 1: ENVIO DE NOTIFICAÇÕES (v2) ---
exports.sendNotification = onDocumentCreated("notifications/{notificationId}", async (event) => {
    const data = event.data.data();
    if (!data) return null;

    const { targetUserId, fromUserId, message: messageText, fromUserName, postId } = data;
    if (targetUserId === fromUserId) return null;

    try {
        const userDoc = await admin.firestore().collection("users").doc(targetUserId).get();
        if (!userDoc.exists) return null;

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return null;

        const message = {
            token: fcmToken,
            notification: { title: fromUserName, body: messageText },
            data: { postId: postId || "", type: data.type || "general" },
            android: {
                priority: "high",
                notification: { channelId: "universe_v3", priority: "high" }
            }
        };

        await admin.messaging().send(message);
        console.log("Notificação enviada com sucesso.");
    } catch (error) {
        console.error("Erro na notificação:", error);
    }
    return null;
});

// --- FUNÇÃO 2: LIMPEZA AUTOMÁTICA E ATUALIZAÇÃO DE CONTADORES ---
exports.cleanupUserData = auth.user().onDelete(async (user) => {
    const uid = user.uid;
    const db = admin.firestore();
    const batch = db.batch();

    console.log(`Limpando dados para o utilizador: ${uid}`);

    try {
        // 1. Diminuir seguidores de quem o utilizador apagado SEGUIA
        const followingQuery = await db.collectionGroup("following").where("uid", "==", uid).get();
        for (const doc of followingQuery.docs) {
            const userSeguidoRef = doc.ref.parent.parent;
            if (userSeguidoRef) {
                // Ajusta o contador no perfil de quem perdeu o seguidor
                batch.update(userSeguidoRef, { followersCount: admin.firestore.FieldValue.increment(-1) });
            }
            batch.delete(doc.ref);
        }

        // 2. Diminuir "seguindo" de quem SEGUIA o utilizador apagado
        const followersQuery = await db.collectionGroup("followers").where("uid", "==", uid).get();
        for (const doc of followersQuery.docs) {
            const seguidorRef = doc.ref.parent.parent;
            if (seguidorRef) {
                // Ajusta o contador no perfil de quem seguia o user apagado
                batch.update(seguidorRef, { followingCount: admin.firestore.FieldValue.increment(-1) });
            }
            batch.delete(doc.ref);
        }

        // 3. Limpar Posts, Comentários e Notificações do utilizador
        const colecoesParaLimpar = ["posts", "notifications", "comments"];
        for (const nomeColl of colecoesParaLimpar) {
            const query = await db.collection(nomeColl).where("userId", "==", uid).get();
            query.forEach(doc => batch.delete(doc.ref));
        }

        // 4. Apagar o perfil do utilizador
        batch.delete(db.collection("users").doc(uid));

        await batch.commit();
        console.log(`Limpeza e contadores atualizados para o UID: ${uid}`);
    } catch (error) {
        console.error("Erro na limpeza automática:", error);
    }
    return null;
});
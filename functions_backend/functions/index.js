const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const { auth } = require("firebase-functions/v1");
const admin = require("firebase-admin");

if (admin.apps.length === 0) {
    admin.initializeApp();
}

// --- FUNÇÃO 1: ENVIO DE NOTIFICAÇÕES ---
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
            android: { priority: "high", notification: { channelId: "universe_v3", priority: "high" } }
        };
        await admin.messaging().send(message);
    } catch (error) { console.error("Erro na notificação:", error); }
    return null;
});

// --- FUNÇÃO 2: LIMPEZA TOTAL AO APAGAR CONTA ---
exports.cleanupUserData = auth.user().onDelete(async (user) => {
    const uid = user.uid;
    const db = admin.firestore();
    const batch = db.batch();

    try {
        // 1. Quem este user seguia perde 1 seguidor (followersCount)
        const followingQuery = await db.collectionGroup("following").where("uid", "==", uid).get();
        followingQuery.forEach(doc => {
            const userSeguidoRef = doc.ref.parent.parent;
            if (userSeguidoRef) {
                // Prevenção de números negativos: usamos o incremento apenas se necessário
                batch.update(userSeguidoRef, { followersCount: admin.firestore.FieldValue.increment(-1) });
            }
            batch.delete(doc.ref);
        });

        // 2. Quem seguia este user perde 1 no "seguindo" (followingCount)
        const followersQuery = await db.collectionGroup("followers").where("uid", "==", uid).get();
        followersQuery.forEach(doc => {
            const seguidorRef = doc.ref.parent.parent;
            if (seguidorRef) {
                batch.update(seguidorRef, { followingCount: admin.firestore.FieldValue.increment(-1) });
            }
            batch.delete(doc.ref);
        });

        // 3. Limpar coleções onde o campo é 'userId' ou 'targetUserId'
        const collections = ["posts", "comments"];
        for (const name of collections) {
            const q = await db.collection(name).where("userId", "==", uid).get();
            q.forEach(d => batch.delete(d.ref));
        }

        // Notificações precisam de verificação dupla (quem enviou e quem recebeu)
        const notifSent = await db.collection("notifications").where("fromUserId", "==", uid).get();
        notifSent.forEach(d => batch.delete(d.ref));
        const notifReceived = await db.collection("notifications").where("targetUserId", "==", uid).get();
        notifReceived.forEach(d => batch.delete(d.ref));

        batch.delete(db.collection("users").doc(uid));

        await batch.commit();
        console.log(`Limpeza completa para o UID: ${uid}`);
    } catch (error) { console.error("Erro na limpeza:", error); }
    return null;
});

// --- FUNÇÃO 3: RECALIBRAGEM TOTAL (FORÇA SINCRONIZAÇÃO REAL) ---
exports.recalibrateCounters = onRequest(async (req, res) => {
    const db = admin.firestore();
    try {
        const usersSnapshot = await db.collection("users").get();
        let totalRepaired = 0;

        // Processamos um a um para evitar timeouts em coleções grandes
        for (const userDoc of usersSnapshot.docs) {
            const userId = userDoc.id;

            // Contagem física real dos documentos nas sub-coleções
            const followersSnapshot = await db.collection("users").doc(userId).collection("followers").get();
            const followingSnapshot = await db.collection("users").doc(userId).collection("following").get();

            const actualFollowers = followersSnapshot.size;
            const actualFollowing = followingSnapshot.size;

            const userData = userDoc.data();

            // Só atualiza se houver erro no contador, garantindo que nunca seja negativo
            if (userData.followersCount !== actualFollowers || userData.followingCount !== actualFollowing) {
                await db.collection("users").doc(userId).update({
                    followersCount: Math.max(0, actualFollowers),
                    followingCount: Math.max(0, actualFollowing)
                });
                totalRepaired++;
            }
        }
        return res.status(200).send(`Recalibragem concluída. ${totalRepaired} perfis sincronizados.`);
    } catch (error) {
        console.error("Erro na recalibragem:", error);
        return res.status(500).send("Erro: " + error.message);
    }
});
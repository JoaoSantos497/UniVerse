const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const { auth } = require("firebase-functions/v1");
const admin = require("firebase-admin");

if (admin.apps.length === 0) {
    admin.initializeApp();
}

// --- FUNÇÃO 1: ENVIO DE NOTIFICAÇÕES ---
exports.sendNotification = onDocumentCreated("notifications/{notificationId}", async (event) => {
    const notificationData = event.data.data();
    if (!notificationData) {
        console.log("Dados da notificação estão vazios.");
        return null;
    }

    // Desestruturação segura dos dados do documento
    const {
        targetUserId,
        fromUserId,
        message: messageText,
        type = "general", // Valor padrão para segurança
        postId = "",     // Valor padrão para segurança
        fromUserName = "UniVerse" // Título padrão
    } = notificationData;

    if (!targetUserId || !fromUserId) {
         console.log("targetUserId ou fromUserId em falta.");
         return null;
    }

    // Previne auto-notificações
    if (targetUserId === fromUserId) {
        console.log("Utilizador tentou notificar-se a si mesmo. Ignorando.");
        return null;
    }

    try {
        // Obtém o token FCM do utilizador que vai receber a notificação
        const userDoc = await admin.firestore().collection("users").doc(targetUserId).get();
        if (!userDoc.exists) {
            console.log(`Utilizador alvo ${targetUserId} não encontrado.`);
            return null;
        }

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) {
            console.log(`Utilizador ${targetUserId} não tem um token FCM.`);
            return null;
        }


        const payload = {
            token: fcmToken,
            notification: {
                title: fromUserName,
                body: messageText
            },
            data: {
                type: type,
                fromUserId: fromUserId,
                postId: postId
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "universe_v3", /
                    priority: "high"
                }
            }
        };

        await admin.messaging().send(payload);
        console.log(`Notificação do tipo '${type}' enviada com sucesso para ${targetUserId}.`);

    } catch (error) {
        console.error("Erro ao enviar notificação:", error);
    }
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

        // 3. Limpar coleções onde o campo é 'userId'
        const collections = ["posts", "comments"];
        for (const name of collections) {
            const q = await db.collection(name).where("userId", "==", uid).get();
            q.forEach(d => batch.delete(d.ref));
        }

        // 4. Limpar notificações enviadas e recebidas
        const notifSent = await db.collection("notifications").where("fromUserId", "==", uid).get();
        notifSent.forEach(d => batch.delete(d.ref));
        const notifReceived = await db.collection("notifications").where("targetUserId", "==", uid).get();
        notifReceived.forEach(d => batch.delete(d.ref));

        // 5. Apagar o documento do utilizador
        batch.delete(db.collection("users").doc(uid));

        await batch.commit();
        console.log(`Limpeza completa para o UID: ${uid}`);
    } catch (error) { console.error("Erro na limpeza de dados do utilizador:", error); }
    return null;
});


// --- FUNÇÃO 3: RECALIBRAGEM TOTAL (FORÇA SINCRONIZAÇÃO REAL) ---
exports.recalibrateCounters = onRequest(async (req, res) => {
    const db = admin.firestore();
    try {
        const usersSnapshot = await db.collection("users").get();
        let totalRepaired = 0;

        for (const userDoc of usersSnapshot.docs) {
            const userId = userDoc.id;

            const followersSnapshot = await db.collection("users").doc(userId).collection("followers").get();
            const followingSnapshot = await db.collection("users").doc(userId).collection("following").get();

            const actualFollowers = followersSnapshot.size;
            const actualFollowing = followingSnapshot.size;

            const userData = userDoc.data();

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

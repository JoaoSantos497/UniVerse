const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { auth } = require("firebase-functions/v1"); // Importação específica da v1
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

// --- FUNÇÃO 2: LIMPEZA AUTOMÁTICA (v1) ---
// Usamos auth.user().onDelete() que é o padrão estável
exports.cleanupUserData = auth.user().onDelete(async (user) => {
    const uid = user.uid;
    const db = admin.firestore();
    const batch = db.batch();

    console.log(`Limpando dados para o utilizador: ${uid}`);

    try {
        // 1. Perfil
        batch.delete(db.collection("users").doc(uid));

        // 2. Posts
        const posts = await db.collection("posts").where("userId", "==", uid).get();
        posts.forEach(doc => batch.delete(doc.ref));

        // 3. Notificações
        const notifications = await db.collection("notifications").where("targetUserId", "==", uid).get();
        notifications.forEach(doc => batch.delete(doc.ref));

        // 4. Seguidores/Seguindo (Collection Groups)
        const followers = await db.collectionGroup("followers").where("uid", "==", uid).get();
        followers.forEach(doc => batch.delete(doc.ref));

        const following = await db.collectionGroup("following").where("uid", "==", uid).get();
        following.forEach(doc => batch.delete(doc.ref));

        await batch.commit();
        console.log(`Limpeza concluída para o UID: ${uid}`);
    } catch (error) {
        console.error("Erro na limpeza automática:", error);
    }
    return null;
});
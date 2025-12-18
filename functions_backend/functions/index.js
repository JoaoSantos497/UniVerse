const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendNotification = onDocumentCreated("notifications/{notificationId}", async (event) => {
    // Na v2, os dados estão em event.data
    const data = event.data.data();

    if (!data) {
        console.log("Sem dados no documento");
        return null;
    }

    const targetUserId = data.targetUserId;
    const messageText = data.message;
    const fromUserName = data.fromUserName;
    const postId = data.postId;

    // 1. Ir buscar o Token do telemóvel do destinatário
    try {
        const userDoc = await admin.firestore().collection("users").doc(targetUserId).get();

        if (!userDoc.exists) {
            console.log("Utilizador não encontrado: " + targetUserId);
            return null;
        }

        const fcmToken = userDoc.data().fcmToken;

        if (!fcmToken) {
            console.log("Utilizador sem fcmToken");
            return null;
        }

        // 2. Criar o pacote da notificação
        const message = {
            token: fcmToken,
            notification: {
                title: fromUserName,
                body: messageText,
            },
            data: {
                postId: postId || "",
                type: data.type || "general"
            }
        };

        // 3. Enviar usando a API moderna (send)
        const response = await admin.messaging().send(message);
        console.log("Notificação enviada com sucesso:", response);
    } catch (error) {
        console.error("Erro ao processar notificação:", error);
    }
    return null;
});
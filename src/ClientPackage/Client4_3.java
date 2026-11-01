package ClientPackage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class Client4_3 {

    public static void main(String[] args) throws Exception {
        // 1) Créer une socket UDP
        DatagramSocket socket = new DatagramSocket();

        // 2) Demander le nom d'utilisateur
        Scanner scanner = new Scanner(System.in);
        System.out.print("Entrez votre nom d'utilisateur : ");
        String nomUtilisateur = scanner.nextLine();

        // Adresse et port du serveur
        InetAddress adresseServeur = InetAddress.getByName("localhost");
        int portServeur = 1234;

        // --- Thread de RÉCEPTION (tourne en arrière-plan) ---
        // Ce thread écoute en permanence les messages envoyés par le serveur
        Thread threadReception = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                try {
                    while (true) {
                        DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(paquet);

                        String messageRecu = new String(paquet.getData(), 0, paquet.getLength());
                        System.out.println(messageRecu);
                    }
                } catch (Exception e) {
                    // Socket fermée à la fin
                }
            }
        });
        threadReception.setDaemon(true);
        threadReception.start();

        // 3) Envoi automatique du message de connexion au serveur
        // En UDP, le serveur ne connaît notre adresse IP/Port que si on lui envoie un paquet.
        // Ce message enregistre immédiatement le client sur le serveur dès son démarrage !
        String messageConnexion = "[" + nomUtilisateur + "] a rejoint le chat !";
        byte[] bytesConnexion = messageConnexion.getBytes();
        socket.send(new DatagramPacket(bytesConnexion, bytesConnexion.length, adresseServeur, portServeur));

        System.out.println("--- Connecté au chat ! (Tapez vos messages, ou 'exit' pour quitter) ---\n");

        // --- Thread principal : LECTURE et ENVOI des messages ---
        while (true) {
            String messageSaisi = scanner.nextLine();

            // Si l'utilisateur tape "exit", on prévient le serveur et on quitte
            if (messageSaisi.equalsIgnoreCase("exit")) {
                String messageDeconnexion = "[" + nomUtilisateur + "] a quitté le chat.";
                byte[] bytesDeconnexion = messageDeconnexion.getBytes();
                socket.send(new DatagramPacket(bytesDeconnexion, bytesDeconnexion.length, adresseServeur, portServeur));
                System.out.println("Vous avez quitté le chat.");
                break;
            }

            // Afficher notre propre message dans notre console
            System.out.println("[Moi]: " + messageSaisi);

            // Formater le message : [NomUtilisateur]: message et envoyer aux autres
            String messageFormate = "[" + nomUtilisateur + "]: " + messageSaisi;
            byte[] donnees = messageFormate.getBytes();
            DatagramPacket paquet = new DatagramPacket(
                    donnees, donnees.length, adresseServeur, portServeur
            );
            socket.send(paquet);
        }

        // Fermer les ressources
        scanner.close();
        socket.close();
    }
}

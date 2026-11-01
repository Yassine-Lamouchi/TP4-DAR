package ServeurPackage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Serveur4_3 {
    // Buffer pour recevoir les données (1024 octets max)
    static byte[] buffer = new byte[1024];

    // Liste synchronisée qui stocke les adresses de tous les clients connectés
    static List<SocketAddress> listeClients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {
        // Créer une socket UDP liée au port 1234
        DatagramSocket socket = new DatagramSocket(null);
        socket.bind(new InetSocketAddress(1234));

        System.out.println("=== Serveur de chat UDP lancé sur le port 1234 ===");
        System.out.println("Le serveur peut aussi écrire des messages pour les envoyer aux clients.");
        System.out.println("En attente de messages...\n");

        // --- Thread pour permettre au SERVEUR de PARLER (écrire des messages) ---
        // Ce thread lit ce que tape l'administrateur dans la console du serveur et l'envoie à tous les clients
        Thread threadEnvoiServeur = new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    String messageServeur = scanner.nextLine();

                    // Afficher dans la console du serveur
                    System.out.println("[Moi (Serveur)]: " + messageServeur);

                    // Formater le message : [Serveur]: message
                    String messageFormate = "[Serveur]: " + messageServeur;
                    byte[] donnees = messageFormate.getBytes();

                    // Diffuser à TOUS les clients connectés
                    synchronized (listeClients) {
                        for (SocketAddress client : listeClients) {
                            try {
                                DatagramPacket paquet = new DatagramPacket(donnees, donnees.length, client);
                                socket.send(paquet);
                            } catch (Exception e) {
                                System.out.println("Erreur d'envoi vers " + client);
                            }
                        }
                    }
                }
            }
        });
        threadEnvoiServeur.start();

        // --- Thread principal : RÉCEPTION et DIFFUSION des messages reçus ---
        while (true) {
            // 1) Attendre et recevoir un paquet UDP
            DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
            socket.receive(paquet);

            // 2) Extraire le message et l'adresse de l'expéditeur
            String message = new String(paquet.getData(), 0, paquet.getLength());
            SocketAddress adresseExpediteur = paquet.getSocketAddress();

            // 3) Si ce client n'est pas encore dans la liste, on l'ajoute
            if (!listeClients.contains(adresseExpediteur)) {
                listeClients.add(adresseExpediteur);
                System.out.println("[NOUVEAU CLIENT] " + adresseExpediteur + " enregistré. (Total clients: " + listeClients.size() + ")");

                // Le serveur souhaite la bienvenue directement au nouveau client
                String msgBienvenue = "[Serveur]: Bienvenue dans le chat !";
                byte[] dataBienvenue = msgBienvenue.getBytes();
                socket.send(new DatagramPacket(dataBienvenue, dataBienvenue.length, adresseExpediteur));
            }

            // 4) Afficher le message reçu côté serveur avec l'adresse
            System.out.println("Reçu de " + adresseExpediteur + " : " + message);

            // Si le client quitte, on le retire de la liste
            if (message.contains("a quitté le chat")) {
                listeClients.remove(adresseExpediteur);
                System.out.println("[DÉCONNEXION] " + adresseExpediteur + " retiré. (Clients restants: " + listeClients.size() + ")");
            }

            // 5) Diffuser le message reçu à TOUS les autres clients (sauf l'expéditeur)
            byte[] donnees = message.getBytes();
            synchronized (listeClients) {
                for (SocketAddress client : listeClients) {
                    if (!client.equals(adresseExpediteur)) {
                        DatagramPacket paquetEnvoi = new DatagramPacket(donnees, donnees.length, client);
                        socket.send(paquetEnvoi);
                    }
                }
            }
        }
    }
}

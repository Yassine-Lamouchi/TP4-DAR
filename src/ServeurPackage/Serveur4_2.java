package ServeurPackage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

class Serveur4_2 {
    static byte buffer[] = new byte[1024];
    public static void main(String argv[]) throws Exception {
        DatagramSocket socket = new DatagramSocket(1234);
        System.out.println("Lancement du serveur");
        while (true) {
            // Le serveur attend un paquet UDP
            DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
            socket.receive(paquet);
            int taille = paquet.getLength();
            String donnees = new String(paquet.getData(), 0, taille);
            System.out.println("Donnees reçues = " + donnees);
        }
    }
}

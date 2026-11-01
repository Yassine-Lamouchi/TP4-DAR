package ClientPackage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class Client4_2 {
    public static void main(String argv[]) throws Exception {
        // Le client crée une socket UDP
        DatagramSocket socket = new DatagramSocket();
        // Prépare un paquet UDP contenant le message,
        // l'adresse du serveur et le port
        String ch = " Mon message ";
        // Le message " Mon message " est converti en tableau
        // d'octets (byte) pour être envoyé via UDP
        int length = ch.length();
        byte buffer[] = ch.getBytes();
        DatagramPacket donneesEmises = new DatagramPacket(buffer, length, InetAddress.getByName("localhost"), 1234);
        // Le client envoie le paquet au serveur
        socket.send(donneesEmises);
    }
}

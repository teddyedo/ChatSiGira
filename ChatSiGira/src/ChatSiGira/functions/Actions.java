/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ChatSiGira.functions;

import ChatSiGira.pacchettipackage.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import ChatSiGira.app.Connection;
import static ChatSiGira.functions.UserInfo.chatRoomList;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import ChatSiGira.graphicinterface.ChatInterface;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.util.List;

/**
 *
 * @author Allari Edoardo
 *
 */
public class Actions {

    /**
     * Constructor.
     */
    public Actions() {
    }

    /**
     * This method is used to perform loginInterface to the server.
     *
     * @param alias the name choosen by the user logged in.
     * @throws IOException
     */
    public static void registration(String alias) throws IOException {

        UserInfo.alias = alias;

        String topic = "general";

        RegistrationPacket r = new RegistrationPacket(alias, topic);

        Connection.os.write(r.toBytes());
        System.out.println("Sended registration request ");

    }

    /**
     * This method is used to change our alias.
     *
     * @param newAlias the new name choosen by the user.
     * @throws IOException
     */
    public static void changeAlias(String newAlias) throws IOException {

        ChangeOfAliasPacket c = new ChangeOfAliasPacket(UserInfo.ID, UserInfo.alias, newAlias);

        Connection.os.write(c.toBytes());

        System.out.println("Sended alias change packet");

    }

    /**
     * This method is used to disconnect our user from the server.
     *
     * @throws IOException
     */
    public static void disconnection() throws IOException {

        DisconnectionClientPacket dscPkt = new DisconnectionClientPacket(UserInfo.ID);

        Connection.os.write(dscPkt.toBytes());

        System.out.println("Sended disconnection request");
    }

    /**
     * This method is used to request the list of the user "online".
     *
     * @throws IOException
     */
    public static void requestUserList() throws IOException {

        GroupUsersListRequestPacket g = new GroupUsersListRequestPacket(UserInfo.ID);

        Connection.os.write(g.toBytes());

        System.out.println("Sended list request");

    }

    /**
     * This method is used to send private message to another user.
     *
     * @param message the content of the message.
     * @param dstAlias the name of the addressee.
     * @throws IOException
     */
    public static void sendedPrivateMex(String message, String dstAlias) throws IOException {

        UtuPacket u = new UtuPacket(UserInfo.ID, dstAlias, message);

        Connection.os.write(u.toBytes());

        System.out.println("Sended private message");

        for (ChatRoom c : chatRoomList) {
            if (c.getAlias().equals(dstAlias)) {
                c.getChatInterface().updateMessageLabel(message, dstAlias);
            }
        }
    }

    /**
     * This method is used to send a topic message to a group of users.
     *
     * @param message the content of the message.
     * @throws IOException
     */
    public static void sendedTopicMex(String message) throws IOException {

        UtCPacket u = new UtCPacket(UserInfo.ID, message);
        System.out.println(u.size());

        Connection.os.write(u.toBytes());

        System.out.println("Sended public message");

        Connection.mainInterface.updateMessageLabel(message, UserInfo.alias);
    }

    /**
     * This method is used to read on the inputStream.
     *
     * @return a byte array from the server.
     * @throws IOException
     */
    public static byte[] read() throws IOException {

        byte[] buffer = new byte[2048];

        Connection.is.read(buffer);

        return buffer;
    }

    /**
     * This method is used to sort every packet received to its specifical
     * action.
     *
     * @param p packet received.
     * @throws IOException
     */
    public static void whatToDo(Packet p) throws IOException {

        switch (p.getOpCode()) {

            case 01:
                System.out.println("messaggio da utente");
                break;
            case 05:
                System.out.println("messaggio da chat");
                break;
            case 11:
                System.out.println("disconnessioneDaServer");
                serverDisconnection(p);
                break;
            case 20:
                System.out.println("Registrazione avvenuta");
                registrationOccured(p);
                break;
            case 51:
                System.out.println("aggiornare lista");
                userListReceived(p);
                break;
            case 255:
                System.out.println("pacchetto di errore");
                errorPacketReceived(p);
                break;
        }

    }

    /**
     * This method is used to confirm the registration and set our unique ID.
     *
     * @param p packet sorted by whatToDo method.
     * @throws IOException
     */
    public static void registrationOccured(Packet p) throws IOException {

        //creation regAck packet
        RegistrationAckPacket regAck = (RegistrationAckPacket) p;

        //alias verification
        if (!regAck.getAliasConfirmation().equals(UserInfo.alias)) {
            registration(UserInfo.alias);
        }

        //set my ID
        UserInfo.ID = regAck.getAssignedId();

        Connection.loginInterface.setVisible(false);
        Connection.mainInterface.setVisible(true);
        Connection.mainInterface.setUsername(UserInfo.alias);

    }

    /**
     * This method is used to set or update the list of the online users.
     *
     * @param p packet sorted by whatToDo method.
     */
    public static void userListReceived(Packet p) {

        //creation userList packet
        GroupUsersListPacket gUsrLst = (GroupUsersListPacket) p;
        
        Gson json = new Gson();

        JsonReader reader = new JsonReader( new StringReader(gUsrLst.getJsonContent()));
        reader.setLenient(true);
        
        Type userListType = new TypeToken<ArrayList<String>>() {
        }.getType();

        List<String> userList = new ArrayList<>();
                        
        userList = json.fromJson(reader, userListType);

        System.out.println(userList.size());

        switch (gUsrLst.getType()) {

            case 0:
                UserInfo.chatUserList.clear();
                for(String s : userList)
                    UserInfo.chatUserList.add(s);
                break;
            case 1:
                UserInfo.chatUserList.add(userList.get(0));
                break;
            case 2:
                UserInfo.chatUserList.remove(userList.get(0));
                break;
            default:
                System.out.println("User list packet error!!");

        }
        
        Connection.mainInterface.updateUserList(UserInfo.chatUserList);
        
    }

    /**
     * This method is used to print out the error message received from the
     * server.
     *
     * @param p packet sorted by whatToDo method.
     */
    public static void errorPacketReceived(Packet p) {

        //creation ErrorPacket 
        ErrorPacket errPkt = (ErrorPacket) p;

        System.out.println(errPkt.getErrorCode().name());
    }

    /**
     * This method is used to accept the disconnection from server and indicate
     * what is the cause of disconnection.
     *
     * @param p packet sorted by whatToDo method.
     */
    public static void serverDisconnection(Packet p) {

        //creation disconnection server packet
        DisconnectionServerPacket dscPkt = (DisconnectionServerPacket) p;

        switch (dscPkt.getReason()) {

            case 0:
                System.out.println("YOU ARE DISCONNECTED FROM SERVER FOR NO REASON");
                break;
            case 1:
                System.out.println("Disconnected for inactivity");
                break;
            case 2:
                System.out.println("Server gone offline");
                break;

        }

    }

    /**
     * This method is used to receive and print out a user message.
     *
     * @param p packet sorted by whatToDo method.
     */
    public static void userMessageReceived(Packet p) {

        //creation utuDPacket 
        UtuDPacket u = (UtuDPacket) p;

        System.out.println(u.getSourceAlias() + ": " + u.getMessage());

        for (ChatRoom c : chatRoomList) {
            if (c.getAlias().equals(u.getSourceAlias())) {
                c.getChatInterface().updateMessageLabel(u.getMessage(), u.getSourceAlias());
            }
        }
    }

    /**
     * This method is used to receive and print out a topic message.
     *
     * @param p packet sorted by whatToDo method.
     */
    public static void topicMessageReceived(Packet p) {

        //creation utcDPacket
        UtcDPacket u = (UtcDPacket) p;

        System.out.println(u.getSourceAlias() + ": " + u.getMessage());

        Connection.mainInterface.updateMessageLabel(u.getMessage(), u.getSourceAlias());
    }

    public static void openPrivateChatRoom(String alias) {

        Boolean created = false;
        
        for(ChatRoom c : chatRoomList){
            if(c.getAlias().equals(alias)){
                System.out.println("A chatRoom with this user is just opened");
                created = true;
            }
        }
        
        if(!created){
            ChatInterface c = new ChatInterface();
            c.setVisible(true);
            c.setTitle(alias);
            ChatRoom chatRoom = new ChatRoom(alias, c);
            chatRoomList.add(chatRoom);
        }
    }

}

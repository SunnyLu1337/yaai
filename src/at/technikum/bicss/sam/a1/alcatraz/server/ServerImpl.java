/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package at.technikum.bicss.sam.a1.alcatraz.server;

import at.technikum.bicss.sam.a1.alcatraz.common.IClient;
import at.technikum.bicss.sam.a1.alcatraz.common.IServer;
import at.technikum.bicss.sam.a1.alcatraz.common.Player;
import at.technikum.bicss.sam.a1.alcatraz.server.spread.PlayerList;
import at.technikum.bicss.sam.a1.alcatraz.server.spread.SpreadServer;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author Rudolf Galler <ic10b039@technikum-wien.at> [1010258039]
 */
public class ServerImpl extends UnicastRemoteObject implements IServer {

    private PlayerList PlayerList;
    private SpreadServer spread_server;

    public ServerImpl() throws RemoteException {
        super();
        // get playerlist with already instanced spread server
        spread_server = SpreadServer.getInstance();
        PlayerList = spread_server.getPlayerList();

    }

    private void broadcastPlayerList() {
        String rmi_adr = null;
        for (Player p : PlayerList) {
            rmi_adr = new String("rmi://" + p.getAddress() + ":" + p.getPort() + "/Alcatraz/ClientImpl/" + p.getName());
            try {
                IClient c = (IClient) Naming.lookup(rmi_adr);
                c.updatePlayerList(PlayerList.getLinkedList());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void register(String name, String address, int port) throws RemoteException {
        Player newPlayer = new Player(name, 0, address, port, false);
        PlayerList.add(newPlayer);
        System.out.println("\nSERVER: Registered new Player:\n" + newPlayer.toString());
        broadcastPlayerList();
    }

    public void deregister(String name) throws RemoteException {
    }

    public void setStatus(String name, boolean ready) throws RemoteException {
                
    }

    @Override
    public String getMasterServer() throws RemoteException {
        return spread_server.getMasterServerAddress();
    }
}

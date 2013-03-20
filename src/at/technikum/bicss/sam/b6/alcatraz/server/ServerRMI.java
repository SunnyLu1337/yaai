/*
 *  yaai - Yet Another Alcatraz Implementation 
 *  BICSS-B6 2013
 */
package at.technikum.bicss.sam.b6.alcatraz.server;

import at.technikum.bicss.sam.b6.alcatraz.common.Player;
import at.technikum.bicss.sam.b6.alcatraz.common.AlcatrazServerException;
import at.technikum.bicss.sam.b6.alcatraz.common.IClient;
import at.technikum.bicss.sam.b6.alcatraz.common.IServer;
import at.technikum.bicss.sam.b6.alcatraz.common.Util;
import at.technikum.bicss.sam.b6.alcatraz.server.spread.PlayerList;
import at.technikum.bicss.sam.b6.alcatraz.server.spread.SpreadServer;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class ServerRMI extends UnicastRemoteObject implements IServer 
{

    private PlayerList player_list;
    private SpreadServer spread_server;
    static private Logger l = Logger.getLogger(Util.getClientRMIPath());

    public ServerRMI() throws RemoteException {
        super();
        // create initial spread server instance
        spread_server = SpreadServer.getInstance();
        player_list = spread_server.getPlayerList();
    }

    private void broadcastPlayerList() {
        player_list.renumberIDs();
        l.info("SERVER: Broadcasting Playerlist");
        for (Player p : player_list) {
            String rmi_uri = Util.buildRMIString(p.getAddress(), p.getPort(),
                    Util.getClientRMIPath(), p.getName());
            l.debug("SERVER: Send Playerlist to " + rmi_uri);
            try {
                IClient c = (IClient) Naming.lookup(rmi_uri);
                c.updatePlayerList(player_list.getLinkedList());
            } catch (Exception e) {
                l.error("SERVER: Error while broadcasting playerlist:\n" + e.getMessage(), e);
            }
        }
    }

    @Override
    public synchronized void register(String name, String address, int port) throws RemoteException, AlcatrazServerException {

        Player newPlayer = new Player(name, 0, address, port, false);
        l.info("SERVER: New player wants to register:\n" + newPlayer.toString());

        for (Player p : player_list) {
            if (p.getName().equals(name)) {
                AlcatrazServerException e =
                        new AlcatrazServerException("Player with name " + name
                        + " already registered.\nName must be unique, "
                        + "please us a different name.");
                l.warn(e.getMessage());
                throw e;
            }
        }
        if (player_list.getLinkedList().size() >= 4) {
            AlcatrazServerException e =
                    new AlcatrazServerException("This game is already full! "
                    + "(max. 4 Players)\nPlease try some time later.");
            l.warn(e.getMessage());
            throw e;
        }

        player_list.add(newPlayer);

        l.info("SERVER: Registered new Player:\n" + newPlayer.toString());
        broadcastPlayerList();
    }

    @Override
    public synchronized void deregister(String name) throws RemoteException, AlcatrazServerException {
        l.info("SERVER: Player wants to deregister" + name);
        Player p_remove = null;
        for (Player p : player_list) {
            if (p.getName().equals(name)) {
                p_remove = p;
            }
        }

        if (p_remove == null) {
            AlcatrazServerException e = new AlcatrazServerException("Playername " + name + " not found!");
            l.warn(e.getMessage());
            throw e;
        } else {
            player_list.remove(p_remove);
            l.info("SERVER: Deregistered Player " + name);
            broadcastPlayerList();
        }
    }

    @Override
    public synchronized void setStatus(String name, boolean ready) throws RemoteException, AlcatrazServerException {
        l.info("Player " + name + " wants to set readystatus to " + ready);
        Player p_status = null;
        for (Player p : player_list) {
            if (p.getName().equals(name)) {
                p_status = p;
            }
        }

        if (p_status == null) {
            AlcatrazServerException e = 
                    new AlcatrazServerException("Playername " + name + " not found!");
            l.warn(e.getMessage());
            throw e;
        } else {
            p_status.setReady(ready);
            broadcastPlayerList();
            if (player_list.allReady()) {
                spread_server.setPlayerList(new LinkedList());
                player_list = spread_server.getPlayerList();
            }
            player_list.triggerObjectChangedEvent();
        }
    }

    @Override
    public String getMasterServer() throws RemoteException {
        return spread_server.getMasterServerAddress();
    }
}
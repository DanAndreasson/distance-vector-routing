import javax.swing.*;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class RouterNode {
  private int id;
  private GuiTextArea gui;
  private RouterSimulator simulator;
  private int[] costs = new int[RouterSimulator.NUM_NODES];
  private int[][] distanceVector = new int[RouterSimulator.NUM_NODES][RouterSimulator.NUM_NODES];
  // Route will keep track of which router we route through to get to a neighbor
  private Map<Integer, Integer> route = new HashMap<>();
  private boolean POISONREVERSE = true;

  public RouterNode(int id, RouterSimulator sim, int[] costs) {
    this.id = id;
    this.simulator = sim;
    this.gui = new GuiTextArea("  Output window for Router #" + this.id + "  ");

    for(int[] v : distanceVector) {
      // It takes infinity to every other node until we know about anything else
      Arrays.fill(v, RouterSimulator.INFINITY);
    }

    System.arraycopy(costs, 0, this.costs, 0, RouterSimulator.NUM_NODES);

    for(int node = 0; node < costs.length; ++node) {
      distanceVector[id][node] = costs[node];

      if(node != id && costs[node] != RouterSimulator.INFINITY)
        route.put(node, node);
    }


    // It costs zero to ourself
    distanceVector[id][id] = 0;

    broadcastUpdate();
  }

  private void broadcastUpdate() {
    route.forEach((dest, through) -> {
      int[] dv = distanceVector[id].clone();

      if (POISONREVERSE) {
        route.forEach((d, t) -> {
          if(d != dest && route.get(d) == dest)
            dv[dest] = RouterSimulator.INFINITY;
        });
      }

      RouterPacket packet = new RouterPacket(id, dest, dv);
      sendUpdate(packet);
    });
  }

  public void updateLinkCost(int dest, int newcost) {
    costs[dest] = newcost;
    distanceVector[id][dest] = newcost;
    route.put(dest, dest);

    broadcastUpdate();
  }

  public void recvUpdate(RouterPacket packet) {
    int fromNode = packet.sourceid;
    boolean changes = false;

    distanceVector[fromNode] = packet.mincost.clone();

    // Loop through our distanceVector and maybe update it
    for (int node = 0; node < distanceVector[id].length; ++node) {
      if (node == id) continue;

      // Does anyone have a cheaper route to node? 
      // Do not check my own DV and only direct neighbors
      int newCost = RouterSimulator.INFINITY;
      int throughNode = -1;
      for( int nbr = 0; nbr < costs.length; ++nbr) {
        if (nbr == id || costs[nbr] == RouterSimulator.INFINITY){
          continue;
        }
        int costThroughNbr = costs[nbr] + distanceVector[nbr][node];  

        if(costThroughNbr < newCost) {
          newCost = costThroughNbr;
          throughNode = nbr;
        }
      }

      if(newCost != distanceVector[id][node]) {
        distanceVector[id][node] = newCost;
        route.put(node, throughNode);
        changes = true;
      }
    }

    if (changes)
      broadcastUpdate();
  }

  private void sendUpdate(RouterPacket pkt) {
    simulator.toLayer2(pkt);
  }

  public void printDistanceTable() {
    gui.println("\n\n");
    gui.println("Current table for " + id + "  at time " + simulator.getClocktime());
    printNeighborDistanceTables();
    printOurDistanceTable();
  }

  private void printNeighborDistanceTables() {
    gui.println("\nDistancetable:");
    printHeader();

    for(int node = 0; node < costs.length; ++node) {
      if(node == id || costs[node] == RouterSimulator.INFINITY) continue;

      gui.print(" nbr  " + node + " |");

      for(int distance : distanceVector[node]) {
        gui.print(String.format("%10d", distance));
      }
      gui.println();
    }
  }

  private void printOurDistanceTable() {
    gui.println("\nOur distance vector and routes:");
    printHeader();

    gui.print(String.format(" %8s|", "cost"));

    for(int cost : costs) {
      gui.print(String.format("%10s", cost));
    }

    gui.println();
    gui.print(String.format(" %8s|", "route"));

    for(int cost : distanceVector[id]) {
      gui.print(String.format("%10d", cost));
    }
  }

  private void printHeader() {
    gui.print("     dst |");
    String divider = "-----";

    for(int nodeID = 0; nodeID < RouterSimulator.NUM_NODES; ++nodeID) {
      gui.print(String.format("%10d", nodeID));
      divider += "------";
    }

    gui.println();
    gui.println(divider);
  }
}

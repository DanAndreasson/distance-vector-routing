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

  public void updateLinkCost(int dest, int newcost) {
    costs[dest] = newcost;
    System.out.printf("LINK COST CHANGE! %s -> %s now costs %s\n", id, dest, newcost);
    calculateCheapest();
  }

  public void recvUpdate(RouterPacket packet) {
    distanceVector[packet.sourceid] = packet.mincost.clone();
    calculateCheapest();
  }

  private void sendUpdate(RouterPacket pkt) {
    simulator.toLayer2(pkt);
  }

  private void calculateCheapest() {
    boolean changes = false;

    // Loop through our distanceVector and maybe update it
    for (int node = 0; node < distanceVector[id].length; ++node) {
      if (node == id) continue;

      // Does anyone have a cheaper route to node? 
      // Do not check my own DV and only direct neighbors
      int cheapest = costs[node];
      int throughNode = node;
      for( int nbr = 0; nbr < costs.length; ++nbr) {
        if (nbr == id || costs[nbr] == RouterSimulator.INFINITY){
          continue;
        }
        int costThroughNbr = costs[nbr] + distanceVector[nbr][node];  

        if(costThroughNbr < cheapest) {
          cheapest = costThroughNbr;
          throughNode = nbr;
        }
      }

      // Do we have a new cheapest route to node?
      if(cheapest != distanceVector[id][node]) {
        System.out.printf("Updated cost for %s -> %s. Prev cost: %s. New cost: %s. Routes via %s\n", id, node, distanceVector[id][node], cheapest, throughNode);
        distanceVector[id][node] = cheapest;
        route.put(node, throughNode);
        changes = true;
      }
    }

    if (changes)
      broadcastUpdate();
  }

  private void broadcastUpdate() {
    route.forEach((dest, through) -> {
      int[] dv = distanceVector[id].clone();

      if (POISONREVERSE) {
        route.forEach((d, t) -> {
          if(d != dest && route.get(d) == dest){
            System.out.printf("%s Lies to %s and tells that %s -> %s is INFINITY because %s routes via %s in order to get to %s. (Actual cost: %s)\n", id, dest, id, d, id, d, dest, costs[d]);
            dv[d] = RouterSimulator.INFINITY;
          }
        });
      }

      RouterPacket packet = new RouterPacket(id, dest, dv);
      sendUpdate(packet);
    });
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
      if(costs[node] == RouterSimulator.INFINITY) continue;

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

    gui.println();
    gui.print(String.format(" %7s |", "cost"));

    for(int cost : costs) {
      gui.print(String.format("%10s", cost));
    }
    gui.println();

    gui.print(String.format(" %7s |", "route"));

    for(int node = 0; node < costs.length; ++node) {
      String nextNode = "-";
      if(route.get(node) != null) 
        nextNode = Integer.toString(route.get(node));
      gui.print(String.format("%10s", nextNode));
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

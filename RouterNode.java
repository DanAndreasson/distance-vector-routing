import javax.swing.*;
import java.util.Arrays;

public class RouterNode {
  private int id;
  private GuiTextArea gui;
  private RouterSimulator simulator;
  private int[] costs = new int[RouterSimulator.NUM_NODES];
  private int[][] distanceVector = new int[RouterSimulator.NUM_NODES][RouterSimulator.NUM_NODES];

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
    }

    // It costs zero to ourself
    distanceVector[id][id] = 0;

    broadcastUpdate();
  }

  private void broadcastUpdate() {
    for (int node = 0; node < costs.length; ++node) {
      // Only send to neighbors
      if(node == id || costs[node] == RouterSimulator.INFINITY) {
        continue;
      }

      // Send our distanceVector (route) to all neighbors
      RouterPacket packet = new RouterPacket(id, node, distanceVector[id]);
      sendUpdate(packet);
    }
  } 

  public void updateLinkCost(int dest, int newcost) {
    // System.out.printf("-- Link cost change between %s and %s now costs %3s \n", id, dest, newcost);
    costs[dest] = newcost;
    distanceVector[id][dest] = newcost;

    broadcastUpdate();
  }

  public void recvUpdate(RouterPacket packet) {
    int fromNode = packet.sourceid;
    boolean changes = false;

    // Update the senders distanceVector
    distanceVector[fromNode] = (int[]) packet.mincost.clone();

    // Loop through our distanceVector and maybe update it
    for (int node = 0; node < distanceVector[id].length; ++node) {
      if (node == id) continue;
      // System.out.println();

      // Loop through all neighbors again
      // Does anyone have a cheaper route to node? 
      int newCost = RouterSimulator.INFINITY;
      for( int nbr = 0; nbr < costs.length; ++nbr) {
        if (nbr == id || costs[nbr] == RouterSimulator.INFINITY){
          continue;
        }
        int costThroughNbr = costs[nbr] + distanceVector[nbr][node];  

        if(costThroughNbr < newCost) {
          // System.out.println("  På " + id + " Billigast genom " + nbr + " till " + node + " för " + costThroughNbr);
          newCost = costThroughNbr;
        }
      }

      if(newCost != distanceVector[id][node]) {
        // System.out.printf("newCost %-5s In node: %-5s To: %-5s Got DV: %-10s from %-5s My Cost %-10s\n", newCost, id, node, Arrays.toString(distanceVector[fromNode]), fromNode, Arrays.toString(costs));
        distanceVector[id][node] = newCost;
        changes = true;
      }
      // else {
      //   System.out.printf("stay at %-5s In node: %-5s To: %-5s Got DV: %-10s from %-5s My Cost %-10s\n", distanceVector[id][node], id, node, Arrays.toString(distanceVector[fromNode]), fromNode, Arrays.toString(costs));
      // }
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
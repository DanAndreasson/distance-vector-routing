import javax.swing.*;
import java.util.Arrays;
import java.lang.Math;

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

  public void recvUpdate(RouterPacket packet) {
    int fromNode = packet.sourceid;
    boolean changes = false;

    // Update the senders distanceVector
    distanceVector[fromNode] = (int[]) packet.mincost.clone();

    // Loop through our distanceVector and maybe update it
    for (int node = 0; node < distanceVector[id].length; ++node) {
      if (node == id) continue;

      int newCost = Math.min(
          costs[fromNode] + distanceVector[fromNode][node],
          distanceVector[id][node]
          );

      if(newCost != distanceVector[id][node]) {
        distanceVector[id][node] = newCost;
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

    int node = 0;
    for(int[] nbr : distanceVector) {
      if(node == id) continue;

      gui.print(" nbr  " + node + " |");

      for(int distance : nbr) {
        gui.print(String.format("%10d", distance));
      }
      gui.println();
      ++node;
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

  public void updateLinkCost(int dest, int newcost) {
  }
}

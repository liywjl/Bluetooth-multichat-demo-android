package com.example.will.finalversion_bluetooth;

import java.util.ArrayList;

/**
 * Created by Will on 03/08/2015.
 */
public class RoutingTable {

    private ArrayList<NodeRout> mRoutingTableArray;


    public RoutingTable(){

        //Inisialise the search for devices button
        mRoutingTableArray= new ArrayList<NodeRout>();

    }


    public void createNewNodeRoute(String nodeName, String nodeAddress,int numberOfHops,String nextNodeHop){
        //create the node route
        NodeRout neighbourDevice = new NodeRout(nodeName,nodeAddress,numberOfHops, nextNodeHop);
        //add the node to the array list
        //addNodeRoute(neighbourDevice);
        mRoutingTableArray.add(neighbourDevice);
    }


    /**
     * This will add a Node Route to the Arraylist, if it is inexsistant or better
     * @param route
     */
    public void addNodeRoute(NodeRout route){
        //add the route if the Array is empty
        if (mRoutingTableArray.isEmpty()){
            mRoutingTableArray.add(route);
        } else {
            //create a boolean to check if the route has been found
            boolean nodeFound = false;
            //get the MAC address of the route node
            String nodeAddreess = route.getNodeAddress();
            //loop through the array of nodes to find if node route already exists
            tableloop:for (int i=0;i<mRoutingTableArray.size();i++){
                //get the MAC address of each node
                String existingNodeAddress = mRoutingTableArray.get(i).getNodeAddress();
                //check exsitance of the node
                if (nodeAddreess==existingNodeAddress){
                    nodeFound = true;
                    //get the number of hops for each route
                    int currentHops = mRoutingTableArray.get(i).getNumberOfHops();
                    int alternativeRoutHops = route.getNumberOfHops();
                    //if the alternative route has less hops insert it as new route
                    if (!(currentHops-alternativeRoutHops<=0)){
                        mRoutingTableArray.add(i,route);
                    }
                    //break the array loop as device found
                    break tableloop;
                }
            }
            //if the node is not found, then add the route to the array
            if(!nodeFound){
                mRoutingTableArray.add(route);
            }
        }
    }


    public void removeNodeRoute(NodeRout route){
        //Check that array is not empty
        if (mRoutingTableArray.isEmpty()){
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //the table is empty so throw exception
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        } else {
            //create a boolean to check if the route has been found
            boolean nodeFound = false;
            //get the MAC address of the route node
            String nodeAddreess = route.getNodeAddress();
            //loop through the array of nodes to find if node route already exists
            tableloop:for (int i=0;i<mRoutingTableArray.size();i++){
                //get the MAC address of each node
                String existingNodeAddress = mRoutingTableArray.get(i).getNodeAddress();
                //check exsitance of the node
                if (nodeAddreess==existingNodeAddress){
                    nodeFound = true;
                    //get the number of hops and make it -1, as it will be considered infanint

                    mRoutingTableArray.get(i).setNumberOfHops(-1);

                    //break the array loop as device found
                    break tableloop;
                }
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                //if the node route is not found throw Exception!!!
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            }
            //if the node is not found, then add the route to the array
            if(!nodeFound){
                //thorw and error saying node rout doesnt exist

            }
        }
    }



    /**
     * searches through the array rout table, and returns a String, null if not found and next hop if found
     * @param nodeAddress
     * @return
     */
    public String getRout(String nodeAddress){
        String nextHop = null;
        routingLoop:for (int i = 0; i<mRoutingTableArray.size();i++){
            String node = mRoutingTableArray.get(i).getNodeAddress();
            if(node==nodeAddress){
                nextHop = mRoutingTableArray.get(i).getNodeAddress();
                break routingLoop;
            }
        }
        return nextHop;
    }
}

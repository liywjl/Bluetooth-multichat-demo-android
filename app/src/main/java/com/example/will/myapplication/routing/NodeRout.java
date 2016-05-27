package com.example.will.myapplication.routing;

/**
 * Created by Will on 04/08/2015.
 */
public class NodeRout{
    private String nodeName;
    private String nodeAddress;
    private int numberOfHops;
    private String nextNodeHop;
    private int sequenceNumber;

    NodeRout(String nodeAddress, int numberOfHops){
        this.nodeAddress=nodeAddress;
        this.numberOfHops=numberOfHops;
    }

    /**
     * Constructor for node route
     */
    NodeRout(String nodeName, String nodeAddress,int numberOfHops,String nextNodeHop){
        this.nodeAddress=nodeAddress;
        this.numberOfHops=numberOfHops;
        this.nextNodeHop=nextNodeHop;
        this.nodeName=nodeName;
    }

    public String getNodeName(){
        return  nodeAddress;
    }

    public String getNodeAddress(){
        return  nodeAddress;
    }

    public String getNextNodeHop(){
        return  nextNodeHop;
    }

    public int getNumberOfHops(){
        return  numberOfHops;
    }

    public void setNumberOfHops(int i){
        numberOfHops = i;
    }


}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.*;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;

/**
 *
 * @author max
 */

// TODO attach network interfaces and volumes to existing instances,tag root device
// change the address type in network interfaces , recognize network interface by 
// bidirectional port and the lable that says NetworkInterface
//availability zone problems in volumes 
public class AwsPushTest {

    private AmazonEC2Client client = null;
    private AwsEC2Get ec2Client = null;
    private String topologyUri = null;
    private Regions region =null;
    static final Logger logger = Logger.getLogger(AwsPush.class.getName());

    public static void main(String[] args) throws Exception {
        String modelAdditionStr = "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n"
                +"<urn:ogf:network:aws.amazon.com:aws-cloud>\n"
                +"         nml:hasTopology <urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e9> .\n"
                +"<urn:ogf:network:aws.amazon.com:aws-cloud:vpcservice-us-east-1>\n"
                +"         mrs:providesVPC <urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e9> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60>\n"
                + "        a                         nml:Node , owl:NamedIndividual ;\n"
                + "        mrs:hasVolume             <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> , <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50be> ;\n"
                + "        mrs:providedByService     <urn:ogf:network:aws.amazon.com:aws-cloud:ec2service-us-east-1> ;\n"
                + "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> , <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e9>\n"
                + "        a                      nml:Topology , owl:NamedIndividual ;\n"
                + "        mrs:hasNetworkAddress  <urn:ogf:network:aws.amazon.com:aws-cloudvpcnetworkaddress-vpc-0c034169> ;\n"
                + "        nml:hasService         <urn:ogf:network:aws.amazon.com:aws-cloud:routingservice-vpc-0c034169> , <urn:ogf:network:aws.amazon.com:aws-cloud:switchingservice-vpc-8c5f22e9> ;\n"
                + "        nml:hasNode     <urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:ec2service-us-east-1>\n"
                + "        mrs:providesVM  <urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:switchingservice-vpc-8c5f22e9>\n"
                + "        a                   mrs:SwitchingService , owl:NamedIndividual .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:subnet-2cd6ad16>\n"
                + "        a       mrs:SwitchingSubnet , owl:NamedIndividual ;\n"
                + "        mrs:hasNetworkAddress <urn:ogf:network:aws.amazon.com:aws-cloud:subnetnetworkaddress-0339843> ;\n"
                + "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> , <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> \n"
                + "        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                + "        nml:labeltype  nml:NetworkInterface;\n"
                + "        mrs:privateIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.230> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> \n"
                + "        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                + "        nml:labeltype  nml:NetworkInterface;\n"
                + "        mrs:privateIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.231> ;\n"
                + "        mrs:publicIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:54.152.72.205> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:ebsservice-us-east-1>\n"
                + "        mrs:providesVolume  <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> , <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50be> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf>\n"
                + "        a          mrs:Volume , owl:NamedIndividual ;\n"
                + "        mrs:value  \"gp2\" ;\n"
                + "        mrs:target_device \"/dev/xvdba\" ;\n"
                + "        mrs:disk_gb \"8\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50be>\n"
                + "        a          mrs:Volume , owl:NamedIndividual ;\n"
                + "        mrs:value  \"standard\" ;\n"
                + "        mrs:target_device \"/dev/xvda\" ;\n"
                + "        mrs:disk_gb \"8\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:vpcnetworkaddress-vpc-0c034169>\n"
                + "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                + "        mrs:type   \"ipv4-prefix\" ;\n" 
                + "        mrs:value  \"10.0.0.0/16\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:routingservice-vpc-0c034169>\n"
                + "        a                         mrs:RoutingService , owl:NamedIndividual ;\n"
                + "        mrs:providesRoute         <urn:ogf:network:aws.amazon.com:aws-cloud:rtb-2f64394a10.0.0.016> ;\n"
                + "        mrs:providesRoutingTable  <urn:ogf:network:aws.amazon.com:aws-cloud:rtb-2f64394a> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:rtb-2f64394a>\n"
                + "        a             mrs:RoutingTable , owl:NamedIndividual ;\n"
                + "        mrs:hasRoute  <urn:ogf:network:aws.amazon.com:aws-cloud:rtb-2f64394a10.0.0.016> ;\n"
                + "        mrs:type      \"main\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:rtb-2f64394a10.0.0.016>\n"
                + "        a              mrs:Route , owl:NamedIndividual ;\n"
                + "        mrs:nextHop    \"local\" ;\n"
                + "        mrs:routeFrom  <urn:ogf:network:aws.amazon.com:aws-cloudroutefrom-rtb-2f64394a10.0.0.0160> ;\n"
                + "        mrs:routeTo    <urn:ogf:network:aws.amazon.com:aws-cloudrouteto-rtb-2f64394a10.0.0.0160> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:routeto-rtb-2f64394a10.0.0.0160>\n"
                +"         a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                +"         mrs:type   \"ipv4-prefix\" ;\n"
                +"         mrs:value  \"10.0.0.0/16\" .\n"
                +" <urn:ogf:network:aws.amazon.com:aws-cloud:subnetnetworkaddress-0339843>\n"
                +"         a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                +"         mrs:type   \"ipv4-prefix\" ;\n"
                +"         mrs:value  \"10.0.0.0/24\" .\n"
                +"<urn:ogf:network:aws.amazon.com:aws-cloudroutefrom-rtb-2f64394a10.0.0.0160>\n"
                +"         a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                +"         mrs:type   \"subnet\" ;\n"
                +"         mrs:value  \"10.0.0.0/16\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.230>\n"
                + "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                + "        mrs:value  \"10.0.0.230\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.231>\n"
                + "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                + "        mrs:value  \"10.0.0.231\" .\n";

        String modelReductionStr = "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60>\n"
                + "        a                         nml:Node , owl:NamedIndividual ;\n"
                + "        mrs:hasVolume             <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> ;\n"
                + "        mrs:providedByService     <urn:ogf:network:aws.amazon.com:aws-cloud:ec2service-us-east-1> ;\n"
                + "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> , <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> .";
        
         OntModel model= AwsModelBuilder.createOntology("","",Regions.US_EAST_1, "urn:ogf:network:aws.amazon.com:aws-cloud");
         StringWriter out = new StringWriter();
         try {
         model.write(out, "TURTLE");
         } catch (Exception e) {
         throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
         }
         String ttl = out.toString();
         System.out.println(ttl);

        AwsPushTest push = new AwsPushTest("", "", Regions.US_EAST_1, "urn:ogf:network:aws.amazon.com:aws-cloud");
        String request = push.pushPropagate(ttl,modelAdditionStr, "");
        System.out.println(request);
        //push.pushCommit(request);
    }

    public AwsPushTest(String access_key_id, String secret_access_key, Regions region, String topologyUri) {
        //have all the information regarding the topology
        ec2Client = new AwsEC2Get(access_key_id, secret_access_key, region);
        client = ec2Client.getClient();
        this.region =region;

        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
    }


    /*************************************************
     function to propagate all the requests 
     **************************************************/
    public String pushPropagate(String modelTtl,String modelAddTtl, String modelReductTtl) throws Exception {
        String requests = "";

        OntModel modelReduct = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        OntModel modelAdd = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        try {
            modelAdd.read(new ByteArrayInputStream(modelAddTtl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall model addition, due to %s", e.getMessage()));
        }
        
        try {
            model.read(new ByteArrayInputStream(modelTtl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall reference model, due to %s", e.getMessage()));
        }

        /*try {
         modelReduct.read(new ByteArrayInputStream(modelReductTtl.getBytes()), null, "TURTLE");
         } catch (Exception e) {
         throw new Exception(String.format("failure to unmarshall model reduction, due to %s", e.getMessage()));
         }*/
        //delete all the instances that need to be created
        requests += deleteInstancesRequests(modelReduct);

        //create all the vpcs that need to be created
        requests += createVpcsRequests(modelAdd);

        //create all the subnets that need to be created
        requests += createSubnetsRequests(model,modelAdd);
        
        /*//create a volume if a volume needs to be created
        requests += createVolumesRequests(modelAdd);
        
        //create network interface if it needs to be created
        requests += createPortsRequests(model,modelAdd);
        
        //create all the nodes that need to be created 
        requests += createInstancesRequests(modelAdd);
        
        //attach volumes that need to be atatched to existing instances
        requests+= attachVolumeRequests(model,modelAdd);*/

        return requests;
    }

    /************************************************************************
     Function to do execute all the requests provided by the propagate method
     ************************************************************************/
    public void pushCommit(String r) {
        String[] requests = r.split("[\\n]");

        for (String request : requests) {
            if (request.contains("TerminateInstancesRequest")) {
                String[] parameters = request.split("\\s+");
                
                String instanceId = getInstanceId(parameters[1]); 
                TerminateInstancesRequest del = new TerminateInstancesRequest();
                del.withInstanceIds(instanceId);
                DeleteTagsRequest tagRequest = new DeleteTagsRequest();
                client.terminateInstances(del);
                ec2Client.getEc2Instances().remove(ec2Client.getInstance(instanceId));
                
                
            } 
            else if(request.contains("CreateVpcRequest")){
                String[] parameters = request.split("\\s+");
                
                CreateVpcRequest vpcRequest= new CreateVpcRequest();
                vpcRequest.withCidrBlock(parameters[1]);
                CreateVpcResult vpcResult = client.createVpc(vpcRequest);
                String vpcId = vpcResult.getVpc().getVpcId();
                
                //create the tag for the vpc
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[2]));
                tagRequest.withResources(vpcId);
                client.createTags(tagRequest);
                ec2Client.getVpcs().add(vpcResult.getVpc());
                
                //tag the routing table of the 
                DescribeRouteTablesResult tablesResult=this.client.describeRouteTables();
                List<RouteTable>routeTables=tablesResult.getRouteTables();
                routeTables.removeAll(ec2Client.getRoutingTables()); //get the new routing table
                RouteTable mainTable = routeTables.get(0);
                tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[3]));
                tagRequest.withResources(mainTable.getRouteTableId());
                client.createTags(tagRequest);
                ec2Client.getRoutingTables().add(mainTable);
            }
            else if (request.contains("CreateVolumeRequest")) {
                String[] parameters = request.split("\\s+");

                CreateVolumeRequest volumeRequest = new CreateVolumeRequest();
                volumeRequest.withVolumeType(parameters[1])
                        .withSize(Integer.parseInt(parameters[2]))
                        .withAvailabilityZone(parameters[3]);

                
                CreateVolumeResult result = client.createVolume(volumeRequest);
                
                Volume volume = result.getVolume();
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[4]));
                tagRequest.withResources(volume.getVolumeId());
                client.createTags(tagRequest);
                ec2Client.getVolumes().add(volume);
                
                
            } else if (request.contains("CreateNetworkInterfaceRequest")) {
                String[] parameters = request.split("\\s+");

                CreateNetworkInterfaceRequest portRequest = new CreateNetworkInterfaceRequest();
                portRequest.withPrivateIpAddress(parameters[1])
                        .withSubnetId(getResourceId(parameters[2]));
                CreateNetworkInterfaceResult portResult = client.createNetworkInterface(portRequest);
                
                NetworkInterface port = portResult.getNetworkInterface();
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[3]));
                tagRequest.withResources(port.getNetworkInterfaceId());
                client.createTags(tagRequest);
                ec2Client.getNetworkInterfaces().add(port);
                
            } else if (request.contains("AssociateAddressRequest")) {
                String[] parameters = request.split("\\s+");

                Address publicIp = ec2Client.getElasticIp(parameters[1]);
                AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest();
                associateAddressRequest.withAllocationId(publicIp.getAllocationId())
                        .withNetworkInterfaceId(getResourceId(parameters[2]));
                client.associateAddress(associateAddressRequest);
                
                
            } else if (request.contains("RunInstancesRequest")) {
               //requests += String.format("RunInstancesRequest ami-146e2a7c t2.micro ") 
                //requests+=String.format("InstanceNetworkInterfaceSpecification %s %d",id,index)
                String[] parameters = request.split("\\s+");

                RunInstancesRequest runInstance = new RunInstancesRequest();
                runInstance.withImageId(parameters[1]);
                runInstance.withInstanceType(parameters[2]);
                runInstance.withMaxCount(1);
                runInstance.withMinCount(1);

                //integrate the root device
                EbsBlockDevice device = new EbsBlockDevice();
                device.withVolumeType(parameters[4]);
                device.withVolumeSize(Integer.parseInt(parameters[5]));
                BlockDeviceMapping mapping = new BlockDeviceMapping();
                mapping.withDeviceName(parameters[6]);
                mapping.withEbs(device);
                String volumeTag = parameters[7];
                runInstance.withBlockDeviceMappings(mapping);

                List<InstanceNetworkInterfaceSpecification> portSpecification = new ArrayList();
                for (int i = 9; i < parameters.length; i++) {
                    InstanceNetworkInterfaceSpecification s = new InstanceNetworkInterfaceSpecification();
                    s.withNetworkInterfaceId(getResourceId(parameters[i]));
                    i++;
                    s.withDeviceIndex(Integer.parseInt(parameters[i]));
                    portSpecification.add(s);
                }
                runInstance.withNetworkInterfaces(portSpecification);
                RunInstancesResult result = client.runInstances(runInstance);
                
                //tag the new instance
                Instance instance =result.getReservation().getInstances().get(0);
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[3]));
                tagRequest.withResources(instance.getInstanceId());
                client.createTags(tagRequest);
                ec2Client.getEc2Instances().add(instance);
                
                //tag the root volume
                tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", volumeTag));
                //String volumeId = m.getEbs().getVolumeId();
                //tagRequest.withResources(volumeId);
                //client.createTags(tagRequest);
            }
            
             else if (request.contains("AttachVolumeRequest")) 
             {
               String[] parameters = request.split("\\s+");  
               
               AttachVolumeRequest volumeRequest = new AttachVolumeRequest();
               volumeRequest.withInstanceId(getInstanceId(parameters[1]))
                       .withVolumeId(getVolumeId(parameters[2]))
                       .withDevice(parameters[3]);
 
               client.attachVolume(volumeRequest);
             }
        }
    }

//function that execustes a query and returns the result
    private ResultSet executeQuery(String queryString, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        //get all the nodes that will be added
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        return (ResultSet) qexec.execSelect();
    }


    /******************************************************************
     function to delete an instance from a model
     ******************************************************************/
    private String deleteInstancesRequests(OntModel modelReduct) throws Exception {
        String requests = "";
        String query = "SELECT ?node WHERE {?node a nml:Node}";
        ResultSet r = executeQuery(query, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("node");
            
            query = "SELECT ?service ?volume ?port WHERE {<"+node.asResource()+"> mrs:providedByService ?service ."
                    + "<"+node.asResource()+"> mrs:hasVolume ?volume ."
                    + "<"+node.asResource()+"> nml:hasBidirectionalPort ?port}";
            ResultSet r1 = executeQuery(query,modelReduct);
            if(!r1.hasNext())
                 throw new Exception(String.format("model reduction is malformed for node: %s", node));
            QuerySolution querySolution1 = r1.next();
            
            RDFNode service =querySolution.get("service");
            RDFNode volume =querySolution.get("volume");
            RDFNode port = querySolution1.get("port");
            if(service == null)
                 throw new Exception(String.format("node to delete does not specify service for node: %s", node ));
            if(volume == null)
                 throw new Exception(String.format("node to delete does not specify volume for node: %s", node ));
            if(port == null)
                 throw new Exception(String.format("node to delete does not specify port for node: %s", node ));
            
            query = "SELECT ?vpc WHERE {?vpc nml:hasNode <"+node.asResource()+">}";
            r1 = executeQuery(query,modelReduct);
            if(!r1.hasNext())
                 throw new Exception(String.format("model reduction does not specify vpc of node: %s",node ));
            
            query = "SELECT ?service WHERE {?service  mrs:providesVM <"+node.asResource()+">}";
            r1 = executeQuery(query,modelReduct);
            if(!r1.hasNext())
                 throw new Exception(String.format("model reduction does not specify service of node: %s",node ));
 
            
            String nodeIdTagValue = node.asResource().toString().replace(topologyUri, "");
            String nodeId = getInstanceId(nodeIdTagValue);

            Instance instance = ec2Client.getInstance(nodeId);
            if (instance == null) //instance does not exists
            {
                throw new Exception(String.format("Node to delete: %s does not exist", node));
            } else {
                requests += String.format("TerminateInstancesRequest %s \n", nodeId);
            }
        }
        return requests;
    }

    /******************************************************************
     Function to create a Vpc from a model
     ******************************************************************/
    private String createVpcsRequests(OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?vpc WHERE {?service mrs:providesVPC  ?vpc ."
                + "?vpc a nml:Topology}";
        ResultSet r = executeQuery(query, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode vpc = querySolution.get("vpc");
            String vpcIdTagValue = vpc.asResource().toString().replace(topologyUri, "");
            String vpcId = getResourceId(vpcIdTagValue);
            
                        //double check vpc does not exist in the cloud
            Vpc v = ec2Client.getVpc(vpcId);
            if (v == null) // vpc does not exist, has to be created
            {
                query = "SELECT ?cloud WHERE {?cloud nml:hasTopology <"+vpc.asResource()+">}";
                ResultSet r1 = executeQuery(query,modelAdd);
                if(!r1.hasNext())
                    throw new Exception(String.format("model addition does not specify the aws-cloud that"
                            + "provides VPC : %s", vpc));

                query = "SELECT ?address WHERE {<"+vpc.asResource()+"> mrs:hasNetworkAddress ?address}";
                r1 = executeQuery(query,modelAdd);
                if(!r1.hasNext())
                    throw new Exception(String.format("model addition does not specify the "
                            + "newtowk address of vpc: %s", vpc));
                QuerySolution querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");

                query = "SELECT ?type ?value WHERE {<"+address.asResource()+"> mrs:type ?type ."
                        + "<"+address.asResource()+"> mrs:value ?value}";
                r1 = executeQuery(query,modelAdd);
                if(!r1.hasNext())
                    throw new Exception(String.format("model addition does not specify the "
                            + "type or value of network address: %s", address));
                querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String cidrBlock = value.asLiteral().toString();


                //check taht vpc offers switching and routing Services and vpc services
                query = "SELECT ?service  WHERE {<"+vpc.asResource()+"> nml:hasService  ?service ."
                        + "?service a  mrs:SwitchingService}";
                r1 = executeQuery(query,modelAdd);
                if(!r1.hasNext())
                    throw new Exception(String.format("New Vpc %s does not speicfy Switching Service", vpc ));
                query = "SELECT ?service  WHERE {<"+vpc.asResource()+"> nml:hasService  ?service ."
                        + "?service a mrs:RoutingService}";
                r1 = executeQuery(query,modelAdd);
                if(!r1.hasNext())
                    throw new Exception(String.format("New Vpc %s does not speicfy Routing Service", vpc ));
                querySolution1 = r1.next();
                RDFNode routingService = querySolution1.get("service");


                //incorporate main routing table and routes into the vpc
                query = "SELECT ?routingTable ?type  WHERE {<"+routingService.asResource()+"> mrs:providesRoutingTable ?routingTable ."
                        + "?routingTable mrs:type  \"main\"}";
                r1 = executeQuery(query,modelAdd);
                if(!r1.hasNext())
                    throw new Exception(String.format("Routing service  %s does not speicfy main Routing table", routingService));

                querySolution1 = r1.next();
                RDFNode routingTable = querySolution1.get("routingTable");
                String routeTableIdTagValue = routingTable.asResource().toString().replace(topologyUri, "");

                //add routeTable id tag to the request to tatg the main route Table later
                requests +=" " + routeTableIdTagValue +"\n";
                cidrBlock = "\""+cidrBlock+"\"";

                //check for  the local route is in the route table 
                 query = "SELECT ?route ?from ?to  WHERE {<"+routingTable.asResource()+"> mrs:hasRoute ?route ."
                         + "<"+routingService.asResource()+"> mrs:providesRoute ?route ."
                         + "?route mrs:nextHop \"local\" ."
                         + "?route mrs:routeFrom ?from ."
                         + "?route mrs:routeTo  ?to ."
                         + "?from mrs:value "+cidrBlock+" ."
                         + "?from mrs:value "+cidrBlock+"}";
                r1 = executeQuery(query,modelAdd);
                if(!r1.hasNext())
                    throw new Exception(String.format("Ruting service has no route for main table", routingService));
                
                requests += String.format("CreateVpcRequest %s %s",cidrBlock,vpcIdTagValue);
            }
        }
        return requests;
    }

    /******************************************************************
     Function to create a subnets from a model
     ******************************************************************/
    private String createSubnetsRequests(OntModel model,OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet}";
        ResultSet r = executeQuery(query, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode subnet = querySolution.get("subnet");
            String subnetIdTagValue = subnet.asResource().toString().replace(topologyUri, "");
            String subnetId = getResourceId(subnetIdTagValue);

            Subnet s = ec2Client.getSubnet(subnetId);

            if (s == null) //subnet does not exist, need to create subnet
            {
               query= "SELECT ?service {?service mrs:providesSubnet <"+subnet.asResource()+">}";
               ResultSet r1 = executeQuery(query, modelAdd);
               if(!r1.hasNext())
                  throw new Exception(String.format("Subnet %s does not specify service",subnet));
               QuerySolution querySolution1 = r1.next();
               RDFNode service = querySolution1.get("service");
               query= "SELECT ?vpc {?vpc nml:hasService <"+service.asResource()+">}";
               r1 = executeQuery(query, modelAdd);
               if(!r1.hasNext())
                   r1 =executeQuery(query, model);
               if(!r1.hasNext())
                   throw new Exception(String.format("Subnet %s does not have a vpc",subnet));
               querySolution1= r1.next();
               RDFNode vpc = querySolution1.get("vpc");
               String vpcIdTagValue = vpc.asResource().toString().replace(topologyUri,"");
               
               query = "SELECT ?subnet ?address ?value WHERE {?subnet mrs:hasNetworkAddress ?address ."
                       + "?address a mrs:NetworkAddress ."
                       + "?address mrs:type \"ipv4-prefix\" ."
                       + "?address mrs:value ?value}";
               r1 = executeQuery(query, modelAdd);
               querySolution1= r1.next();
               RDFNode value  = querySolution1.get("value");
               String cidrBlock = value.asLiteral().toString();
               
               requests += String.format("CreateSubnetRequest %s %s %s",vpcIdTagValue,cidrBlock,subnetIdTagValue);
                       
            }
        }
        return requests;
    }

    /******************************************************************
     Function to create a volumes from a model
     ******************************************************************/
    private String createVolumesRequests(OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?volume WHERE {?volume a mrs:Volume}";
        ResultSet r = executeQuery(query, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode volume = querySolution.get("volume");
            String volumeIdTagValue = volume.asResource().toString().replace(topologyUri, "");
            String volumeId = getVolumeId(volumeIdTagValue);

            Volume v = ec2Client.getVolume(volumeId);

            if (v == null) //volume does not exist, need to create a volume
            {
                //check what service is providing the volume
                query = "SELECT ?type WHERE {?service mrs:providesVolume <" + volume.asResource() + ">}";
                ResultSet r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify service that provides volume: %s", volume));
                }

                //find out the type of the volume
                query = "SELECT ?type WHERE {<" + volume.asResource() + "> mrs:value ?type}";
                r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify new type of volume: %s", volume));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode type = querySolution1.get("type");

                //find out the size of the volume
                query = "SELECT ?size WHERE {<" + volume.asResource() + "> mrs:disk_gb ?size}";
                r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify new size of volume: %s", volume));
                }
                querySolution1 = r1.next();
                RDFNode size = querySolution1.get("size");

                //dont create the volumes with no target device and that are attached to an instance
                //since this volumes are the root devices
                query = "SELECT ?deviceName WHERE {<" + volume.asResource() + "> mrs:target_device ?deviceName}";
                r1 = executeQuery(query, modelAdd);
                if (r1.hasNext()) {
                    querySolution1 = r1.next();
                    String deviceName = querySolution1.get("deviceName").asLiteral().toString();

                    if (!deviceName.equals("/dev/sda1") && !deviceName.equals("/dev/xvda")) {
                        requests += String.format("CreateVolumeRequest %s %s %s  %s  \n", type.asLiteral().getString(),
                                size.asLiteral().getString(), region.getName() + "e", volumeIdTagValue);
                    }
                }
                else
                {
                    requests += String.format("CreateVolumeRequest %s %s %s %s  \n", type.asLiteral().getString(),
                                size.asLiteral().getString(), Regions.US_EAST_1.getName() + "e", volumeIdTagValue);
                }
            }
        }

        return requests;
    }

    /******************************************************************
     Function to create network interfaces from a model
     ******************************************************************/
    private String createPortsRequests(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?port WHERE {?port a  nml:BidirectionalPort ."
                + "?port  nml:labeltype   nml:NetworkInterface}";
        ResultSet r = executeQuery(query, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode port = querySolution.get("port");
            String portIdTagValue = port.asResource().toString().replace(topologyUri, "");
            String portId = getResourceId(portIdTagValue);

            NetworkInterface p = ec2Client.getNetworkInterface(portId);

            if (p == null) //network interface does not exist, need to create a network interface
            {
                //to get the private ip of the network interface
                query = "SELECT ?address WHERE {<" + port.asResource() + ">  mrs:privateIpAddress  ?address}";
                ResultSet r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify privat ip address of port: %s", port));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");
                String privateAddress = address.asResource().toString().replace(topologyUri, "");

                //find the subnet that has the port previously found
                query = "SELECT ?subnet WHERE {?subnet  nml:hasBidirectionalPort <" + port.asResource() + ">}";
                r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not network subnet of port: %s", port));
                }
                String subnetId = null;
                while(r1.hasNext())
                {
                    querySolution1= r1.next();
                    RDFNode subnet = querySolution1.get("subnet");
                    query = "SELECT ?subnet WHERE {<"+subnet.asResource()+">  a  mrs:SwitchingSubnet}";
                    ResultSet r3= executeQuery(query,model);
                    if(r3.hasNext()) //search in the model to see if subnet existed before
                    {
                        subnetId = subnet.asResource().toString().replace(topologyUri, "");
                        subnetId = getResourceId(subnetId);
                    }
                    else //subnet should be specified in modelAdd
                    {
                        r3= executeQuery(query,modelAdd);
                        if(r3.hasNext())
                        {
                            subnetId = subnet.asResource().toString().replace(topologyUri, "");
                            subnetId = getResourceId(subnetId);
                        }
                    }
                }
                
                if(subnetId == null)
                    throw new Exception(String.format("model additions subnet for port %s"
                            + "is not found in the reference model", subnetId));

                //create the network interface 
                requests += String.format("CreateNetworkInterfaceRequest  %s %s %s \n", privateAddress, subnetId, portIdTagValue);

                //to get the public ip address of the network interface if any
                query = "SELECT ?address WHERE {<" + port.asResource() + ">  mrs:publicIpAddress  ?address}";
                r1 = executeQuery(query, modelAdd);
                if (r1.hasNext()) {
                    querySolution1 = r1.next();
                    address = querySolution1.get("address");
                    query = "SELECT ?value WHERE {<" + address.asResource() + ">  mrs:value  ?value}";
                    ResultSet r2 = executeQuery(query, model);
                    if(!r2.hasNext())
                        throw new Exception(String.format("model additions network addres  %s for port $s"
                                + "is not found in the reference mode",address,port));
                    QuerySolution querySolution2 = r2.next();
                    RDFNode value= querySolution2.get("value");
                    String publicAddress = value.asLiteral().toString().replace(topologyUri, "");
                    requests += "AssociateAddressRequest " + publicAddress + " " + portIdTagValue
                             + " \n";
                }
            }
        }
        return requests;
    }

    /******************************************************************
     Function to create Instances
     ******************************************************************/
    private String createInstancesRequests(OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?node WHERE {?node a nml:Node}";
        ResultSet r = executeQuery(query, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("node");
            String nodeIdTagValue = node.asResource().toString().replace(topologyUri, "");
            String nodeId = getInstanceId(nodeIdTagValue);

            Instance instance = ec2Client.getInstance(nodeId);
            List<Instance> i =ec2Client.getEc2Instances();
            if (instance == null) //instance needs to be created
            {
                //check what service is providing the instance
                query = "SELECT ?service WHERE {?service mrs:providesVM <" + node.asResource() + ">}";
                ResultSet r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify service that provides Instance: %s", node));
                }
                

                //find the Vpc that the node will be in
                query = "SELECT ?vpc WHERE {?vpc nml:hasNode <" + node.asResource() + ">}";
                r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the Vpc of the node: %s", node));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode vpc = querySolution1.get("vpc");
                String vpcId = vpc.asResource().toString().replace(topologyUri, "");
                vpcId = getResourceId(vpcId);

                //to find the subnet the node is in first  find the port the node uses
                query = "SELECT ?port WHERE {<" + node.asResource() + "> nml:hasBidirectionalPort ?port}";
                ResultSet r2 = executeQuery(query, modelAdd);
                if (!r2.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the subnet that the node is: %s", node));
                }
                List<String> portsId = new ArrayList();
                RDFNode lastPort = null;
                while (r2.hasNext())//there could be multiple network interfaces attached to the instance
                {
                    QuerySolution querySolution2 = r2.next();
                    RDFNode port = querySolution2.get("port");
                    String id = port.asResource().toString().replace(topologyUri, "");
                    portsId.add(getResourceId(id));
                    lastPort = port;
                }

                //find the EBS volumes that the instance uses
                query = "SELECT ?volume WHERE {<" + node.asResource() + ">  mrs:hasVolume  ?volume}";
                ResultSet r4 = executeQuery(query, modelAdd);
                if (!r4.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the volume of the new node: %s", node));
                }
                List<String> volumesId = new ArrayList();
                while (r4.hasNext())//there could be multiple volumes attached to the instance
                {
                    QuerySolution querySolution4 = r4.next();
                    RDFNode volume = querySolution4.get("volume");
                    String id = volume.asResource().toString().replace(topologyUri, "");
                    volumesId.add(getVolumeId(id));
                }

                //put request for new instance
                requests += String.format("RunInstancesRequest ami-146e2a7c t2.micro %s ", nodeIdTagValue);

                //put the root device of the instance
                query = "SELECT ?volume ?deviceName ?size ?type  WHERE {"
                        + "<" + node.asResource() + ">  mrs:hasVolume  ?volume ."
                        + "?volume mrs:target_device ?deviceName ."
                        + "?volume mrs:disk_gb ?size ."
                        + "?volume mrs:value ?type}";
                r4 = executeQuery(query, modelAdd);
                boolean hasRootVolume=false;
                while (r4.hasNext()) {
                    QuerySolution querySolution4 = r4.next();
                    RDFNode volume = querySolution4.get("volume");
                    String volumeTag = volume.asResource().toString().replace(topologyUri, "");
                    String type = querySolution4.get("type").asLiteral().toString();
                    String size = querySolution4.get("size").asLiteral().toString();
                    String deviceName = querySolution4.get("deviceName").asLiteral().toString();
                    if (deviceName.equals("/dev/sda1") || deviceName.equals("/dev/xvda"))
                    {
                        hasRootVolume=true;
                        requests += String.format("%s %s %s %s ", type, size, deviceName, volumeTag);
                    }
                }
                if(hasRootVolume==false)
                throw new Exception(String.format("model addition does not specify root volume for node: %s",node)); 

                int index = 0;
                //put the networ interfaces 
                requests += "NetworkInterfaceSpecification ";
                for (String id : portsId) {
                    requests += String.format("%s %d ", id, index);
                    index++; //increment the device index
                }
                requests += "\n";
            }
        }
        return requests;
    }
    
    
    /******************************************************************
        Attach a volume to an existing instance AWS
    ******************************************************************/
    private String attachVolumeRequests(OntModel model, OntModel modelAdd) throws Exception
    {
        String requests="";
        OntModel modelToUse = modelAdd;
        
        //check if the volume is new therefore it should be in the model additiom
        String query = "SELECT  ?node ?volume  WHERE {?node  mrs:hasVolume  ?volume}";
        
        ResultSet r1= executeQuery(query,modelAdd);
        while(r1.hasNext())
        {
           QuerySolution querySolution1 = r1.next();
           RDFNode node = querySolution1.get("node");
           String nodeTagId= node.asResource().toString().replace(topologyUri,"");
           RDFNode volume = querySolution1.get("volume");
           String volumeTagId= volume.asResource().toString().replace(topologyUri,"");
           String volumeId = getVolumeId(volumeTagId);
           
           
           query = "SELECT ?deviceName WHERE{<"+volume.asResource()+"> mrs:target_device ?deviceName}";
           ResultSet r2 = executeQuery(query,modelToUse);
           if(!r2.hasNext())
               throw new Exception(String.format("volume device name is not specified for volume %s",volume));
           
           QuerySolution querySolution2= r2.next();
           
           RDFNode deviceName = querySolution2.get("deviceName");
           String device= deviceName.asLiteral().toString();
           
           if (!device.equals("/dev/sda1") && !device.equals("/dev/xvda"))
           {
                String nodeId= getInstanceId(nodeTagId);


                Instance ins = ec2Client.getInstance(nodeId);
                Volume vol = ec2Client.getVolume(volumeId);
                if(vol==null)
                {
                  query = "SELECT ?deviceName WHERE{<"+volume.asResource()+"> a mrs:Volume ."
                        + "<"+volume.asResource()+"> mrs:target_device ?deviceName ."
                        + "<"+volume.asResource()+"> mrs:disk_gb ?size ."
                        + "<"+volume.asResource()+"> mrs:value ?type}";
                  r2 = executeQuery(query,modelToUse);
                }
                if(ins!=null)
                {
                    List< InstanceBlockDeviceMapping> map =ins.getBlockDeviceMappings();
                    boolean attach =true;
                    if(vol==null)
                    {
                     requests+=String.format("AttachVolumeRequest %s %s %s \n",nodeId,volumeTagId,device);
                    }
                    else 
                    {
                        for(InstanceBlockDeviceMapping m :map)
                        {
                            String instanceVolumeId = m.getEbs().getVolumeId();
                            if(instanceVolumeId.equals(volumeId))
                            {
                                attach=false;
                                break;
                            }
                        }
                        if(attach == true)
                        {
                            requests+=String.format("AttachVolumeRequest %s %s %s \n",nodeId,volumeTagId,device);
                        }
                    }
                }
                else if (ins == null)
                {
                   requests+=String.format("AttachVolumeRequest %s %s %s \n",nodeTagId,volumeTagId,device);
                }
           }
        }
        return requests;
    }
    
    /******************************************************************
    //function that checks if the id provided is a tag; if it is, it will return
    //the resource id, otherwise it will return the input parameter which is the 
    //id of the resource 
    //NOTE: does not work with volumes and instances because sometimes the tags
    //      persist for a while in this resources, therefore we need to check if the 
    //      instance or volume actually exists
    ******************************************************************/
    private String getResourceId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = ec2Client.getClient().describeTags(tagRequest).getTags();
        if (!descriptions.isEmpty()) {
            return descriptions.get(descriptions.size()-1).getResourceId(); //get the last resource tagged with this id 
        }

        return tag;
    }
    
    /******************************************************************
    //function to get the Id from and instance tag
    * *****************************************************************/
    private String getInstanceId(String tag)
    {   
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);
        
        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = ec2Client.getClient().describeTags(tagRequest).getTags();
        if (!descriptions.isEmpty()) {
            for(TagDescription des : descriptions)
            {
                Instance i = ec2Client.getInstance(des.getResourceId());
                if(i !=null && i.getState().getCode()!=48) //check if the instance was not deleted
                    return des.getResourceId();
            }
        }
        return tag;
    }
    
    /******************************************************************
    //function to get the Id from and instance tag
    ******************************************************************/
    private String getVolumeId(String tag)
    {   
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);
        
        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = ec2Client.getClient().describeTags(tagRequest).getTags();
        if (!descriptions.isEmpty()) {
            for(TagDescription des : descriptions)
            {
                Volume vol = ec2Client.getVolume(des.getResourceId());
                if(vol !=null  && !vol.getState().equals("deleted") )
                return des.getResourceId();
            }
        }
        return tag;
    }
}


 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.*;
import net.maxgigapop.mrs.driver.openstack.OpenStackModelBuilder;

/*
 *
 * @author muzcategui
 */
public class AwsModelBuilder 
{    
    public static OntModel createOntology(String access_key_id,String secret_access_key, Regions region, String topologyURI) throws IOException
    {
        Logger logger = Logger.getLogger(AwsModelBuilder.class.getName());
        
        //create model object
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF); 
        
        //set all the model prefixes
        model.setNsPrefix("rdfs",RdfOwl.getRdfsURI());
        model.setNsPrefix("rdf", RdfOwl.getRdfURI());
        model.setNsPrefix("xsd", RdfOwl.getXsdURI());
        model.setNsPrefix("owl", RdfOwl.getOwlURI());
        model.setNsPrefix("nml", Nml.getURI());
        model.setNsPrefix("mrs", Mrs.getURI());
        
        //set the global properties
        Property hasNode =Nml.hasNode;
        Property hasBidirectionalPort=Nml.hasBidirectionalPort;
        Property hasService =Nml.hasService;
        Property providesVM =Mrs.providesVM;
        Property type = Mrs.type;
        Property providedByService =Mrs.providedByService;
        Property providesBucket =Mrs.providesBucket;
        Property providesRoute =Mrs.providesRoute;
        Property providesSubnet =Mrs.providesSubnet;
        Property providesVPC = Mrs.providesVPC;
        Property providesVolume = Mrs.providesVolume;
        Property routeFrom = Mrs.routeFrom;
        Property routeTo =Mrs.routeTo;
        Property nextHop=Mrs.nextHop;
        Property value =Mrs.value;
        Property name=Nml.name;
        Property hasBucket=Mrs.hasBucket;
        Property hasVolume=Mrs.hasVolume;
        Property hasTopology=Nml.hasTopology;
        Property publicIpAddress=model.createProperty(model.getNsPrefixURI("mrs")+"publicIpAddress");
        Property privateIpAddress=model.createProperty(model.getNsPrefixURI("mrs")+"privateIpAddress");
        Property targetDevice= model.createProperty(model.getNsPrefixURI("mrs")+"target_device");
        Property hasRoute =Mrs.hasRoute;
        Property hasLabel = Nml.hasLabel;
        Property hasNetworkAddress = Mrs.hasNetworkAddress;
        Property providesRoutingTable= model.createProperty(model.getNsPrefixURI("mrs")+"providesRoutingTable");
        
        //set the global resources
        Resource route=Mrs.Route;
        Resource hypervisorService=Mrs.HypervisorService;
        Resource virtualCloudService =Mrs.VirtualCloudService;
        Resource routingService = Mrs.RoutingService;
        Resource blockStorageService = Mrs.BlockStorageService;
        Resource bucket = Mrs.Bucket;
        Resource volume = Mrs.Volume;
        Resource topology =Nml.Topology;
        Resource networkAddress=Mrs.NetworkAddress;
        Resource switchingSubnet=Mrs.SwitchingSubnet;
        Resource switchingService=Mrs.SwitchingService;
        Resource node =Nml.Node;
        Resource port = Nml.BidirectionalPort;
        Resource namedIndividual = model.createResource(model.getNsPrefixURI("mrs")+"NamedIndividual");
        Resource awsTopology = RdfOwl.createResource(model,topologyURI,topology);
        Resource objectStorageService=Mrs.ObjectStorageService;
        Resource routingTable = Mrs.routingTable;
        
        //get the information from the AWS account
        AwsEC2Get ec2Client=new AwsEC2Get(access_key_id,secret_access_key,region);
        AwsS3Get s3Client=new AwsS3Get(access_key_id,secret_access_key,region);
        AwsDCGet dcClient= new AwsDCGet(access_key_id,secret_access_key,region);
        
        
        //create the outer layer of the aws model
        Resource ec2Service=RdfOwl.createResource(model,topologyURI+  ":ec2service-"+region.getName(),hypervisorService);
        Resource vpcService=RdfOwl.createResource(model,topologyURI + ":vpcservice-"+region.getName(),virtualCloudService);
        Resource s3Service= RdfOwl.createResource(model,topologyURI + ":s3service-"+region.getName(),objectStorageService);
        Resource ebsService= RdfOwl.createResource(model,topologyURI + ":ebsservice-"+region.getName(),blockStorageService);
        
        model.add(model.createStatement(awsTopology,hasService,ec2Service));
        model.add(model.createStatement(awsTopology, hasService, vpcService));
        model.add(model.createStatement(awsTopology,hasService,s3Service));
        model.add(model.createStatement(awsTopology,hasService,ebsService));
        
        
        
        //put all the Internet gateways into the model
        for(InternetGateway t: ec2Client.getInternetGateways())
        {
            String internetGatewayId = ec2Client.getIdTag(t.getInternetGatewayId());
            Resource INTERNETGATEWAY = RdfOwl.createResource(model,topologyURI + ":" + internetGatewayId,port);
            Resource IGW_LABEL= RdfOwl.createResource(model,topologyURI + ":igwLabel-" + internetGatewayId,Nml.Label);
            model.add(model.createStatement(IGW_LABEL,Nml.labeltype,Nml.InternetGateway));
            model.add(model.createStatement(IGW_LABEL,value,"any"));
            model.add(model.createStatement(INTERNETGATEWAY, hasLabel,IGW_LABEL));
        }
        
        //put all the Vpn gateways into the model
        for(VpnGateway g : ec2Client.getVirtualPrivateGateways())
        {
            String vpnGatewayId = ec2Client.getIdTag(g.getVpnGatewayId());
            Resource VPNGATEWAY = RdfOwl.createResource(model,topologyURI + ":" + vpnGatewayId,port);
            Resource VPNGW_LABEL= RdfOwl.createResource(model,topologyURI + ":vpngwLabel-" + vpnGatewayId,Nml.Label);
            model.add(model.createStatement(VPNGW_LABEL,Nml.labeltype,Nml.VpnGateway));
            model.add(model.createStatement(VPNGW_LABEL,value,"any"));
            model.add(model.createStatement(VPNGATEWAY, hasLabel,VPNGW_LABEL));
        }
        
        //put all the vpcs and their information into the model
        for(Vpc v : ec2Client.getVpcs())
        {
            String vpcId= ec2Client.getIdTag(v.getVpcId());
            Resource VPC = RdfOwl.createResource(model,topologyURI +":" + vpcId,topology);
            Resource VPC_NETWORK_ADDRESS = 
                    RdfOwl.createResource(model,topologyURI + ":vpcnetworkaddress-"+v.getVpcId(),networkAddress);
            model.add(model.createStatement(vpcService,providesVPC,VPC));
            model.add(model.createStatement(awsTopology,hasTopology,VPC));
            model.add(model.createStatement(VPC_NETWORK_ADDRESS,type,"ipv4-prefix"));
            model.add(model.createStatement(VPC_NETWORK_ADDRESS,value,v.getCidrBlock()));
            model.add(model.createStatement(VPC,hasNetworkAddress,VPC_NETWORK_ADDRESS));
            
            
            //put all the subnets within the vpc
            Resource SWITCHINGSERVICE= RdfOwl.createResource(model,topologyURI + ":switchingservice-" +vpcId,switchingService);
            model.add(model.createStatement(VPC, hasService,SWITCHINGSERVICE));
            for(Subnet p: ec2Client.getSubnets(v.getVpcId()))
            {
                String subnetId= ec2Client.getIdTag(p.getSubnetId());
                Resource SUBNET= RdfOwl.createResource(model,topologyURI + ":" +subnetId,switchingSubnet);
                model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));
                 Resource SUBNET_NETWORK_ADDRESS = 
                 RdfOwl.createResource(model,topologyURI + ":subnetnetworkaddress-"+p.getSubnetId(),networkAddress); 
                model.add(model.createStatement(SUBNET_NETWORK_ADDRESS,type,"ipv4-prefix"));
                model.add(model.createStatement(SUBNET_NETWORK_ADDRESS,value,p.getCidrBlock()));
                model.add(model.createStatement(SUBNET,hasNetworkAddress,SUBNET_NETWORK_ADDRESS));
            
                //put all the intances inside this subnet into the model if there are any
                List<Instance> instances = ec2Client.getInstances(p.getSubnetId());
                if(!instances.isEmpty())
                {
                    for(Instance i : instances)
                    {
                        String instanceId= ec2Client.getIdTag(i.getInstanceId());
                        Resource INSTANCE= RdfOwl.createResource(model,topologyURI + ":" + instanceId ,node);
                        model.add(model.createStatement(VPC,hasNode, INSTANCE));
                        model.add(model.createStatement(ec2Service,providesVM,INSTANCE));
                        model.add(model.createStatement(INSTANCE, providedByService,ec2Service));
                        
                        //put all the voumes attached to this instance into the modle
                        for(Volume vol : ec2Client.getVolumesWithAttachement(i))
                        {
                           String volumeId= ec2Client.getIdTag(vol.getVolumeId());
                           Resource VOLUME= RdfOwl.createResource(model,topologyURI + ":" +volumeId,volume);
                           model.add(model.createStatement(ebsService,providesVolume, VOLUME));
                           model.add(model.createStatement(INSTANCE,hasVolume, VOLUME));
                           model.add(model.createStatement(VOLUME, value,vol.getVolumeType()));
                           model.add(model.createStatement(VOLUME, Mrs.disk_gb,Integer.toString(vol.getSize())));
                           List<VolumeAttachment> volAttach= vol.getAttachments();
                           for(VolumeAttachment va : volAttach)
                           {
                               if(va.getInstanceId().equals(i.getInstanceId()))
                               {
                                   model.add(model.createStatement(VOLUME,targetDevice,va.getDevice()));
                               }
                           }
                        }

                        //put all the network interfaces of each instance into the model
                        for(InstanceNetworkInterface n : AwsEC2Get.getInstanceInterfaces(i))
                        {
                            String portId= ec2Client.getIdTag(n.getNetworkInterfaceId());
                            Resource PORT = RdfOwl.createResource(model,topologyURI + ":" +portId,port);
                            Resource PORT_LABEL= RdfOwl.createResource(model,topologyURI + ":portLabel-" + portId,Nml.Label);
                            model.add(model.createStatement(PORT_LABEL,Nml.labeltype,Nml.NetworkInterface));
                            model.add(model.createStatement(PORT_LABEL,value,"any"));
                            model.add(model.createStatement(PORT, hasLabel,PORT_LABEL));
                            model.add(model.createStatement(INSTANCE,hasBidirectionalPort,PORT));
                            model.add(model.createStatement(SUBNET,hasBidirectionalPort,PORT));
                            
                            
                            //put the private ip (if any) of the network interface in the model
                            for(InstancePrivateIpAddress q : n.getPrivateIpAddresses())
                            {
                                if(q.getPrivateIpAddress()!=null)
                                {
                                    Resource PRIVATE_ADDRESS= RdfOwl.createResource(model,topologyURI + ":" +q.getPrivateIpAddress(),networkAddress);
                                    model.add(model.createStatement(PORT, privateIpAddress, PRIVATE_ADDRESS));
                                    model.add(model.createStatement(PRIVATE_ADDRESS, type, "ipv4-prefix-list"));
                                    model.add(model.createStatement(PRIVATE_ADDRESS,value,q.getPrivateIpAddress()));
                                }
                            }
                            
                            //put the public Ip (if any) of the network interface into the model
                            if(n.getAssociation() !=null && n.getAssociation().getPublicIp()!=null)
                            {
                                model.add(model.createStatement(PORT, publicIpAddress,topologyURI + ":" +n.getAssociation().getPublicIp()));
                               
                            }
                        }
                    }
                }
            }  
            
            //Make the L3 routing model for this VPC
            Resource ROUTINGSERVICE=RdfOwl.createResource(model,topologyURI + ":routingservice-"+ v.getVpcId() ,routingService);
            model.add(model.createStatement(VPC, hasService,ROUTINGSERVICE));
            
            int index =0;
            for(RouteTable t : ec2Client.getRoutingTables(v.getVpcId()))
            {
                List<RouteTableAssociation> associations = t.getAssociations();
                String routeTableId= ec2Client.getIdTag(t.getRouteTableId());
                Resource ROUTINGTABLE = RdfOwl.createResource(model,topologyURI + ":" + routeTableId,routingTable);
                model.add(model.createStatement(ROUTINGSERVICE,providesRoutingTable,ROUTINGTABLE));
                boolean main = t.getAssociations().get(0).getMain();
                if(main ==true)
                    model.add(model.createStatement(ROUTINGTABLE,type,"main"));
                else
                    model.add(model.createStatement(ROUTINGTABLE,type,"local"));
                List<Route> routes= t.getRoutes();
                    for(Route r : routes)
                    {
                        Resource ROUTE_TO=null;
                        Resource ROUTE_FROM=null;
                        int i=0;
                        String routeId = t.getRouteTableId()+ r.getDestinationCidrBlock().replace("/","");
                        Resource ROUTE = RdfOwl.createResource(model,topologyURI + ":" +routeId,route);
                        model.add(model.createStatement(ROUTINGSERVICE,providesRoute,ROUTE));
                        do //get the routes from the amazon cloud to any destination
                        {
                          String target= r.getGatewayId();
                          InternetGateway internetGateway =ec2Client.getInternetGateway(target);
                          VpnGateway vpnGateway = ec2Client.getVirtualPrivateGateway(target);
                          model.add(model.createStatement(ROUTINGTABLE, hasRoute,ROUTE));
                          
                          if(internetGateway!=null)
                          {
                            Resource resource = model.getResource(topologyURI +":"+ ec2Client.getIdTag(internetGateway.getInternetGatewayId()));
                            model.add(model.createStatement(ROUTE,nextHop,resource));
                          }
                          else if(vpnGateway!=null)
                          {
                            Resource resource = model.getResource(topologyURI +":"+ ec2Client.getIdTag(vpnGateway.getVpnGatewayId()));
                            model.add(model.createStatement(ROUTE,nextHop,resource)); 
                          }
                          else
                            model.add(model.createStatement(ROUTE,nextHop,"local")); 

                          ROUTE_TO= RdfOwl.createResource(model,topologyURI + "routeto-" +routeId + i, networkAddress);
                          if(target.equals("local"))
                              model.add(model.createStatement(ROUTE_TO,type,"ipv4-prefix"));
                          else
                              model.add(model.createStatement(ROUTE_TO,type,"ipv4-prefix-list"));
                          
                          model.add(model.createStatement(ROUTE_TO,value,r.getDestinationCidrBlock()));
                          model.add(model.createStatement(ROUTE,routeTo, ROUTE_TO));
                          String p =r.getOrigin();
                          if(r.getOrigin().equals("EnableVgwRoutePropagation"))
                          {
                              List<PropagatingVgw> propagating = t.getPropagatingVgws();
                              for(PropagatingVgw pw : propagating)
                              {
                                  String gatewayId = pw.getGatewayId();
                                  VpnGateway gateway= ec2Client.getVirtualPrivateGateway(gatewayId);
                                  List<VpcAttachment> atta =gateway.getVpcAttachments();
                                  for(VpcAttachment at : atta)
                                  {
                                      if(at.getVpcId().equals(v.getVpcId()))
                                      {
                                          String gwId = ec2Client.getIdTag(gateway.getVpnGatewayId());
                                          ROUTE_FROM= RdfOwl.createResource(model,topologyURI + ":routefrom-" +routeId +i, networkAddress);
                                          model.add(model.createStatement(ROUTE_FROM,type,"gateway")); 
                                          model.add(model.createStatement(ROUTE_FROM,value,topologyURI+":"+gwId)); 
                                          model.add(model.createStatement(ROUTE,routeFrom,ROUTE_FROM));
                                      }
                                  }
                              }
                          }
                          else
                          {
                             ROUTE_FROM= RdfOwl.createResource(model,topologyURI + ":routefrom-" +routeId + i, networkAddress);
                             model.add(model.createStatement(ROUTE_FROM,type,"subnet")); 

                             String subnetId= associations.get(i).getSubnetId();
                             if(subnetId==null)
                                 model.add(model.createStatement(ROUTE_FROM,value,v.getCidrBlock())); 
                             else
                             {
                                 Subnet sub= ec2Client.getSubnet(subnetId);
                                 model.add(model.createStatement(ROUTE_FROM,value,sub.getCidrBlock())); 
                             }

                              model.add(model.createStatement(ROUTE,routeFrom,ROUTE_FROM));
                          }
                              
                          i++; //increment the association index
                          index++; //increment the index for route Ids
                        }while(i<associations.size());

                    }
            }
        }
        
        //put the volumes of the ebsService into the model
        for(Volume v : ec2Client.getVolumesWithoutAttachment())
        {
            String volumeId= ec2Client.getIdTag(v.getVolumeId());
            Resource VOLUME= RdfOwl.createResource(model,topologyURI + ":" +volumeId,volume);
            model.add(model.createStatement(ebsService,providesVolume, VOLUME));
            model.add(model.createStatement(VOLUME, value,v.getVolumeType()));
            model.add(model.createStatement(VOLUME, Mrs.disk_gb,Integer.toString(v.getSize())));
        }
        
        //put all the elastic ips under the account into the model
        for(Address ip : ec2Client.getElasticIps())
        {
            Resource PUBLIC_ADDRESS= RdfOwl.createResource(model,topologyURI + ":" +ip.getPublicIp(),networkAddress);
            model.add(model.createStatement(PUBLIC_ADDRESS, type, "ipv4-prefix-list"));
            model.add(model.createStatement(PUBLIC_ADDRESS,value,ip.getPublicIp()));
        }

        //put all the buckets of the s3Service into the model
        for(Bucket b: s3Client.getBuckets())
        {
            Resource BUCKET = RdfOwl.createResource(model,topologyURI + ":" + b.getName(),bucket);
            model.add(model.createStatement(s3Service, providesBucket,BUCKET));
            model.add(model.createStatement(awsTopology,hasBucket,BUCKET));
        }
        logger.log(Level.INFO, "Ontology model for AWS driver rewritten");
        return model;
    }
}

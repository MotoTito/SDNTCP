package edu.gatech.cs6250.SDNTCP;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFRequest;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.protocol.ver13.OFFactoryVer13;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.python.antlr.ast.Tuple;

import com.google.common.util.concurrent.ListenableFuture;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFMessageWriter;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;

import net.floodlightcontroller.core.IFloodlightProviderService;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;
import net.floodlightcontroller.packet.Ethernet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.topology.NodePortTuple;

public class SDNTCPController implements IOFMessageListener, IOFMessageWriter,
		IFloodlightModule, IOFSwitchListener {

	protected IFloodlightProviderService floodlightProvider;
	protected IStatisticsService statisticProvider;
	protected IOFSwitchService switchProvider;
	protected IStaticFlowEntryPusherService flows;
	protected LinkedList tcpFlowList;
	protected static Logger logger;
	protected OFFactory myFactory ;
	private Map<NodePortTuple, SwitchPortBandwidth> portMap;
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return SDNTCPController.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IStatisticsService.class);
		l.add(IOFSwitchService.class);
		l.add(IStaticFlowEntryPusherService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		statisticProvider = context.getServiceImpl(IStatisticsService.class);
		switchProvider = context.getServiceImpl(IOFSwitchService.class);
		tcpFlowList = new LinkedList();
		logger = LoggerFactory.getLogger(SDNTCPController.class);
		myFactory = OFFactories.getFactory(OFVersion.OF_13);
		flows = context.getServiceImpl(IStaticFlowEntryPusherService.class);
		// TODO Auto-generated method stub

	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		portMap = statisticProvider.getBandwidthConsumption();l
	}

	@Override
	public boolean write(OFMessage m) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<OFMessage> write(Iterable<OFMessage> msgList) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R extends OFMessage> ListenableFuture<R> writeRequest(
			OFRequest<R> request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <REPLY extends OFStatsReply> ListenableFuture<List<REPLY>> writeStatsRequest(
			OFStatsRequest<REPLY> request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		switch (msg.getType()){
		case PACKET_IN:
			/* Retrieve the deserialized packet in message*/
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			String socket;
			
			if (eth.getEtherType() == EthType.IPv4){
				IPv4 ipv4 = (IPv4) eth.getPayload();
				IPv4Address srcIp = ipv4.getSourceAddress();
				IPv4Address dstIp = ipv4.getDestinationAddress();
				
				if (ipv4.getProtocol().equals( IpProtocol.TCP)){
					TCP tcp = (TCP) ipv4.getPayload();
					
					
					TransportPort srcPort = tcp.getSourcePort();
					TransportPort dstPort = tcp.getDestinationPort();
					Short shortFlags = tcp.getFlags();
					String flags = Integer.toBinaryString(0xFFFF & shortFlags);		
					Integer src = Integer.parseInt(srcIp.toString().replace(".", ""));
					Integer dst = Integer.parseInt(dstIp.toString().replace(".", ""));
					if ( src < dst ){
						 socket = srcIp.toString() + ":" + srcPort.toString() + "-" + dstIp.toString() + ":" + dstPort.toString();
					}
					else{
						 socket = dstIp.toString() + ":" + dstPort.toString() + "-" + srcIp.toString() + ":" + srcPort.toString();
					}
					logger.info("TCP Flow From "+ socket + " on switch: " +sw.getId().toString() + " Flags: " + flags);
					
					
					
					if (!tcpFlowList.contains(socket) && flags.equals("10")){
						logger.info("Adding Flow: " + socket);
						tcpFlowList.add(socket);
						Path out = Paths.get("/TCPFlows.txt");
						try {
							Files.write(out,tcpFlowList,Charset.defaultCharset());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return Command.STOP;
//						Match myMatch = myFactory.buildMatch()
//								.setExact(MatchField.ETH_TYPE, EthType.IPv4)
//								.setExact(MatchField.IPV4_SRC, dstIp)
//								.setExact(MatchField.IPV4_DST, srcIp)
//								.setExact(MatchField.IP_PROTO, IpProtocol.TCP)
//								.build();
//						ArrayList<OFAction> actionList = new ArrayList<OFAction>();		
//						OFActions allActions = myFactory.actions();
//						OFOxms oxms = myFactory.oxms();
//						OFActionOutput output = allActions.buildOutput()
//								.
//								.build();
//						
//						OFInstructions instructions = myFactory.instructions();
//						
//						actionList.add(output);
//						OFInstructionApplyActions applyActions = instructions.buildApplyActions()
//								.setActions(actionList)
//								.build();
//						ArrayList<OFInstruction> instructionList = new ArrayList<OFInstruction>();
//						instructionList.add(applyActions);
//						OFFlowAdd flow = ((org.projectfloodlight.openflow.protocol.OFFlowAdd.Builder) myFactory.buildFlowAdd()
//								.setMatch(myMatch)								
//								.setInstructions(instructionList)
//								.setBufferId(OFBufferId.NO_BUFFER)).build();
//						
//						sw.write(flow);
					}
					//Remove TCP Flow from the list if the Packet has a FIN flag set
					else if (tcpFlowList.contains(socket) && shortFlags % 2 == 1){
						logger.info("Removing Flow: " + socket);
						tcpFlowList.remove(socket);
						Path out = Paths.get("/TCPFlows.txt");
						try {
							Files.write(out,tcpFlowList,Charset.defaultCharset());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return Command.STOP;
					}
					//Remove TCP Flow from the list if the Packet has the RST flag set
					else if (tcpFlowList.contains(socket) && flags.length() >= 3 && flags.substring(flags.length()-3, flags.length()-2).equals("1")){
						logger.info("Removing Flow: " + socket);
						tcpFlowList.remove(socket);
						Path out = Paths.get("/TCPFlows.txt");
						try {
							Files.write(out,tcpFlowList,Charset.defaultCharset());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return Command.STOP;
					}
					//Check to see if the packet has the ACK flag set and change the window size then send to it's destination.
					if (flags.length() >= 5 && flags.substring(flags.length()-5, flags.length()-4).equals("1")){
						tcp.setWindowSize((short) 47);
						tcp.resetChecksum();
						ipv4.setPayload(tcp);
						ipv4.resetChecksum();
						eth.setPayload(ipv4);
						eth.resetChecksum();
						byte[] serializedData = eth.serialize();
						OFPacketOut po = sw.getOFFactory().buildPacketOut() /* mySwitch is some IOFSwitch object */
							    .setData(serializedData)
							    .setActions(Collections.singletonList((OFAction) sw.getOFFactory().actions().output(OFPort.FLOOD, 0xffFFffFF)))
							    .setInPort(OFPort.CONTROLLER)
							    .build();
							  
							sw.write(po);
							return Command.STOP;
					}
					else {
						OFPacketOut po = sw.getOFFactory().buildPacketOut() /* mySwitch is some IOFSwitch object */
							    .setData(eth.serialize())
							    .setActions(Collections.singletonList((OFAction) sw.getOFFactory().actions().output(OFPort.FLOOD, 0xffFFffFF)))
							    .setInPort(OFPort.CONTROLLER)
							    .build();
							  
							sw.write(po);
							return Command.STOP;
					}
				}
			}		
			break;
			default:
				break;
				
				
				
		}
		return Command.CONTINUE;
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchRemoved(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchActivated(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port,
			PortChangeType type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchChanged(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}
}

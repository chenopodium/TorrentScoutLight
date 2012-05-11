package com.vaadin.graphics.canvas.widgetset.client.ui;

import com.vaadin.terminal.gwt.client.UIDL;

public class VConnector extends VLine {
	
	private VPort fromPort;
	private VPort toPort;
	
	public VConnector(VPoint start, VPoint end) {
		super(start, end);
		this.setRole("CONNECTOR");
	}
	
	public VConnector(VPort fromPort, VPort toPort) {
		super(fromPort.getCenter(), toPort.getCenter());
		this.fromPort = fromPort;
		this.toPort = toPort;
	}
	
	public VConnector(UIDL uidl){
		super(uidl);
		this.setId(uidl.getStringAttribute("elementid"));
		this.setGroupId(uidl.getStringAttribute("groupid"));
		update(uidl);
	}
	
	public VConnector(UIDL uidl, String id, String groupId){
		super(uidl, id, groupId);
		update(uidl);
	}
	
	public void update(UIDL uidl){
		super.update(uidl);
	}

	public VPort getFromPort() {
		return fromPort;
	}

	public void setFromPort(VPort fromPort) {
		this.fromPort = fromPort;
	}

	public VPort getToPort() {
		return toPort;
	}

	public void setToPort(VPort toPort) {
		this.toPort = toPort;
	}
	
}

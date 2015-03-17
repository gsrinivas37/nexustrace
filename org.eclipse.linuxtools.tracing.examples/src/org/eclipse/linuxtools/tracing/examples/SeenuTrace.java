package org.eclipse.linuxtools.tracing.examples;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfEventParser;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

public class SeenuTrace extends TmfTrace implements ITmfEventParser {

	private long nbEvents = 10000;
	ITmfLocation currentLoc = null;

	@Override
	public IStatus validate(IProject project, String path) {
		try{
			if(new Path(path).getFileExtension().equalsIgnoreCase("seenu")) {
				return Status.OK_STATUS;
			}
		}catch(Exception e){}

		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, path
				+ " is not a valid seenu trace");
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type, String name, String traceTypeId) throws TmfTraceException {
		super.initTrace(resource, path, type, name, traceTypeId);
		setTimeRange(new TmfTimeRange(new TmfTimestamp(0), new TmfTimestamp(1)));
		seekEvent(0);
	}

	@Override
	public long getStreamingInterval() {
		return 0;
	}

	@Override
	public ITmfLocation getCurrentLocation() {
		return currentLoc;
	}

	@Override
	public double getLocationRatio(ITmfLocation location) {
		TmfLongLocation locationInfo = (TmfLongLocation) location;
		return locationInfo.getLocationInfo()/nbEvents;
	}

	@Override
	public ITmfContext seekEvent(ITmfLocation location) {
		TmfLongLocation longLocation = (TmfLongLocation) location;

		if(longLocation==null) {
			longLocation = new TmfLongLocation(0);
		}

		TmfContext context = new TmfContext(longLocation);
		context.setRank(longLocation.getLocationInfo());

		return context;
	}

	@Override
	public ITmfContext seekEvent(double ratio) {
		long loc = (long) (ratio*nbEvents);
		TmfLongLocation location = new TmfLongLocation(loc);
		return seekEvent(location);
	}

	@Override
	public long getNbEvents() {
		return nbEvents;
	}

	@Override
	public synchronized ITmfEvent getNext(ITmfContext context) {
		TmfLongLocation location = (TmfLongLocation) context.getLocation();
		Long info = location.getLocationInfo();
		TmfEvent event = null;

		if(info<getNbEvents()){
			final TmfEventField tmfEventField = new TmfEventField("value", info, null);
			final TmfEventField[] events = new TmfEventField[1];
			events[0] = tmfEventField;
			final TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, events);
			event = new TmfEvent(this, info, new TmfTimestamp(info, ITmfTimestamp.MILLISECOND_SCALE), new TmfEventType(getTraceTypeId(), content), content);
			currentLoc = new TmfLongLocation(++info);
			if (event != null) {
				updateAttributes(context, event.getTimestamp());
				context.setLocation(getCurrentLocation());
				context.increaseRank();
			}
		}

		return event;
	}

	@Override
	public ITmfEvent parseEvent(ITmfContext context) {
		return null;
	}
}

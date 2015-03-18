package org.eclipse.linuxtools.tracing.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
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

	ITmfLocation currentLoc = null;

	TmfLongLocation fCurrent;

	private int fOffset;
	private File fFile;
	private String[] fEventTypes;
	private FileChannel fFileChannel;
	private MappedByteBuffer fMappedByteBuffer;
	private int[] offset = new int[649];

	private static final int CHUNK_SIZE = 65536;

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

		fFile = new File(path);
		fFile.length();
		fEventTypes = readHeader(fFile);
		for(int i=0; i<649; i++){
			offset[i] = -1;
		}

		try {
			fFileChannel = new FileInputStream(fFile).getChannel();
			seek(0);
		} catch (IOException e) {
		}
	}

	@Override
	public long getNbEvents() {
		return 648;
	}

	private String[] readHeader(File file) {
		String header = new String();
		try (BufferedReader br = new BufferedReader(new FileReader(file));) {
			header = br.readLine();
		} catch (IOException e) {
		}
		fOffset = header.length() + 1;
		return header.split(","); //$NON-NLS-1$
	}

	private void seek(long rank) throws IOException {
		final int position = fOffset;
		int size = Math.min((int) (fFileChannel.size() - position), CHUNK_SIZE);
		fMappedByteBuffer = fFileChannel.map(MapMode.READ_ONLY, position, size);
		offset[0]= 0;
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
		return locationInfo.getLocationInfo()/getNbEvents();
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
		long loc = (long) (ratio*getNbEvents());
		TmfLongLocation location = new TmfLongLocation(loc);
		return seekEvent(location);
	}

	@Override
	public synchronized ITmfEvent getNext(ITmfContext context) {
		TmfLongLocation location = (TmfLongLocation) context.getLocation();
		Long info = location.getLocationInfo();
		TmfEvent event = null;
		StringBuffer buffer;

		if(offset[info.intValue()]!=-1){
			fMappedByteBuffer.position(offset[info.intValue()]);
		}else{
		    System.out.println(info);
		    offset[info.intValue()]= fMappedByteBuffer.position();
		}

		if(fMappedByteBuffer.position()+fEventTypes.length>fMappedByteBuffer.limit()){
			setNbEvents(info);
		}

		if(info<getNbEvents()){
			buffer= new StringBuffer();
			String str;
			final TmfEventField[] events = new TmfEventField[fEventTypes.length];
			byte b[] = new byte[1];
			for(int i=0; i< events.length; i++){
				buffer = new StringBuffer();
				fMappedByteBuffer.get(b);
				str = new String(b);
				while(!str.equals(",")){
					if((str.equals("\n")&&i==events.length-1)||(str.equals("\r")&&i==events.length-1)){
						break;
					}else{
					    System.out.println("");
					}

				    buffer.append(str);

				    if(fMappedByteBuffer.position()==fMappedByteBuffer.limit()){
				    	str = "\n";
				    }else{
				    	fMappedByteBuffer.get(b);
				    	str = new String(b);
				    }
				};
				events[i] = new TmfEventField(fEventTypes[i], buffer.toString(), null);
			}

			final TmfEventField tmfEventField = new TmfEventField("value", info, null);
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

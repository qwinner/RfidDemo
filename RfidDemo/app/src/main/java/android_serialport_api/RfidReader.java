package android_serialport_api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import android.os.Handler;

import hyzk.rfiddemo.ExtApi;


public class RfidReader {
	
	private static RfidReader instance;
	
	public static final int STATE_ADDITEM = 1;
	public static final int STATE_ADDHIGH = 2;
	
	private SerialPort mSerialPort = null;

	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;
	private byte[] databuf=new byte[1024];
	private int datasize=0;
	private int cmdid=0;
	private boolean bread=true;
	
	private Handler msgHandler=null;
	
	public static RfidReader getInstance() {
    	if(null == instance) {
    		instance = new RfidReader();
    	}
    	return instance;
    }
	
	private class ReadThread extends Thread {
		@Override
		public void run() {
			super.run();
			while(!isInterrupted()/*true*/) {
				int size;
				try {
					byte[] buffer = new byte[64];
					if (mInputStream == null) return;
					size = mInputStream.read(buffer);
					if (size > 0) {
						onDataReceived(buffer, size);
					}
				} catch (IOException e) {
					e.printStackTrace();	
					return;
				}
			}
		}
	}
	
	public void SetMessageHandler(Handler handler){
    	msgHandler=handler;
    }
	
	private void SendMssage(int cmd,int state,int size,byte[] buffer){
		if(msgHandler!=null)
			msgHandler.obtainMessage(cmd,state,size,buffer).sendToTarget();
    }
	
	public void openSerialPort(){
    	try {
			mSerialPort = getSerialPort();
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();

			/* Create a receiving thread */
			mReadThread = new ReadThread();
			mReadThread.start();
		} catch (SecurityException e) {
			//DisplayError(R.string.error_security);
		} catch (IOException e) {
			//DisplayError(R.string.error_unknown);
		} catch (InvalidParameterException e) {
			//DisplayError(R.string.error_configuration);
		}
    }
	
	public SerialPort getSerialPort() throws SecurityException, IOException, InvalidParameterException {
		if (mSerialPort == null) {
			//RFID
			String path = "/dev/ttyMT2";
			int baudrate = 115200;
			if ( (path.length() == 0) || (baudrate == -1)) {
				throw new InvalidParameterException();
			}
			mSerialPort = new SerialPort(new File(path), baudrate, 0);
		}
		return mSerialPort;
	}
	
	public void closeSerialPort() {
		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
		}
	}
	
	protected void onDataReceived(final byte[] buffer, final int size) {
		System.arraycopy(buffer, 0, databuf,datasize,size);					
		datasize=datasize+size;

		if(CheckCmd()){
			switch(cmdid){
			case 0x03:
				SendCmd(0x02);
				break;
			case 0x02:
				CheckData();
				break;
			}
		}
	}
	
	private boolean CheckCmd(){
		if(databuf[0]==(byte)0x5A&&databuf[1]==(byte)0xA5){
			int size=(byte)(databuf[4])+((databuf[3]<<8)&0xFF00)+9;
			if(datasize>=size){
				return true;
			}
		}
		return false;
	}
	
	private void CheckData(){
		if(!bread)
			return;		
		if(databuf[0]==(byte)0x5A&&databuf[1]==(byte)0xA5){
			int size=(byte)(databuf[4])+((databuf[3]<<8)&0xFF00)+9;
			long msn=0;
			int msv=0;
			if(datasize>=size){
				for(int i=7;i<datasize;){
					if(databuf[i]==(byte)0x89){
						int id = databuf[i+6] & 0xFF;
						id |= ((databuf[i+5] << 8) & 0xFF00);  
					    id |= ((databuf[i+4] << 16) & 0xFF0000);  
					    id |= ((databuf[i+3] << 24) & 0xFF000000); 
						long sn=0x8900000000l+id;
						int sv=databuf[i+9]&0x00FF;
						if(sv>msv){
							msv=sv;
							msn=sn;
						}
						//txt=txt+String.valueOf(sn)+":"+String.valueOf(sv)+"\n";
						//AddItem(/*"���:"+*/String.valueOf(sn),"ǿ��:"+String.valueOf(sv));
						String sns=String.valueOf(sn);
						SendMssage(STATE_ADDITEM,1,sv,sns.getBytes());
						i+=10;
					}else{
						i++;
					}
				}
				
				//editText.setText(String.valueOf(msn));
				if(msn>0){
					String hs=String.valueOf(msn);
					SendMssage(STATE_ADDHIGH,1,hs.length(),hs.getBytes());
					bread=false;
				}
				
			}
		}
	}
	
	public void SendCmd(int id){
		
		byte[] b = new byte[7];
		b[0]=(byte) 0x5A;
		b[1]=(byte) 0xA5;
		b[2]=(byte) id;
		b[3]=(byte) 0x00;
		b[4]=(byte) 0x00;
		b[5]=(byte) 0x01;
		b[6]=(byte) 0x01;
		
		bread=true;
		datasize=0;
		cmdid=id;
		int a= ExtApi.CRC_XModem(b);
		byte[] c=new byte[9];
		System.arraycopy(b,0, c, 0, 7);
		c[7]=(byte)((a>>8)&0x00FF);
		c[8]=(byte)(a&0x00FF);
		try {
			mOutputStream.write(c);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}

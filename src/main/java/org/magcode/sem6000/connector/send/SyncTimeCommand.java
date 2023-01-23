package org.magcode.sem6000.connector.send;

import com.github.sem2mqtt.bluetooth.sem6000.SendingException;
import java.time.LocalDateTime;

public class SyncTimeCommand extends Command {

	public SyncTimeCommand() throws SendingException {
		this(LocalDateTime.now());
	}

	public SyncTimeCommand(LocalDateTime now) throws SendingException {
		int month = now.getMonthValue();
		int day = now.getDayOfMonth();
		int hour = now.getHour();
		int minute = now.getMinute();
		int second = now.getSecond();

		byte[] payload = new byte[9];
		payload[0] = (byte) second;
		payload[1] = (byte) minute;
		payload[2] = (byte) hour;
		payload[3] = (byte) day;
		payload[4] = (byte) month;
		payload[5] = (byte) ((now.getYear() >> 8) & 0xff);
		payload[6] = (byte) (now.getYear() & 0xFF);
		payload[7] = (byte) 0x00;
		payload[8] = (byte) 0x00;

		byte[] message = buildMessage("0100", payload);
		setMessage(message);
	}
}
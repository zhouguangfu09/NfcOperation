package com.axabox.readclubcard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.Charset;

@SuppressLint({"NewApi"})
public class NfcOperation{
	private static final String TAG = "NfcOperation";

	public static NdefMessage createTagAsNdef(String info, String mineType) {
		return new NdefMessage(new NdefRecord[]{new NdefRecord((short)2, mineType.getBytes(Charset.forName("UTF-8")), new byte[0], info.getBytes())});
	}

	public static String getHexString(byte[] bytes) throws Exception {
		String hexStr = "";

		for(int i = 0; i < bytes.length; ++i) {
			hexStr = hexStr + Integer.toString(256 + (255 & bytes[i]), 16).substring(1);
		}

		return hexStr;
	}

	private static NdefMessage[] readNdefMsg(Intent intent) {
		NdefMessage[] ndefMessages = null;
		if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			Parcelable[] parcelable = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			Log.d(TAG, "readNdefMsg rawMsgs:" + parcelable);
			if(parcelable == null) {
				return new NdefMessage[]{new NdefMessage(new NdefRecord[]{new NdefRecord((short)5, new byte[0], new byte[0], new byte[0])})};
			}

			ndefMessages = new NdefMessage[parcelable.length];

			for(int i = 0; i < parcelable.length; ++i) {
				ndefMessages[i] = (NdefMessage)parcelable[i];
			}
		}

		return ndefMessages;
	}

	public static String readTagMsg(Intent intent) {
		return new String(readNdefMsg(intent)[0].getRecords()[0].getPayload());
	}

	public static int writeTag(Activity activity, NfcAdapter nfcAdapter, Tag tag, String info, String mineType) {
		if(tag != null) {
			Log.d("NfcOperation", "tag is not null, write tag use NDEF.writeNdefMessage()");
			int ret = writeTag(createTagAsNdef(info, mineType), tag);
			if(ret == 0) {
				Log.d("NfcOperation", "Success: Wrote unique tag to nfc tag");
			} else {
				Log.e(TAG, "Write failed");
			}
			return ret;
		} else {
			Log.d(TAG, "tag is null, write tag use setNdefPushMessage()");
			nfcAdapter.setNdefPushMessage(createTagAsNdef(info, mineType), activity, new Activity[0]);

			return 0;
		}
	}

	private static int writeTag(NdefMessage ndefMessage, Tag tag) {
		int length = ndefMessage.toByteArray().length;
		Ndef ndef = Ndef.get(tag);
		if(ndef != null) {
			try {
				ndef.connect();
				if(ndef.isWritable()) {
					if(ndef.getMaxSize() < length) {
						Log.e(TAG, "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + length + " bytes.");
						return 1;
					}

					ndef.writeNdefMessage(ndefMessage);
//					ndef.makeReadOnly(); 
					
					return 0;
				}
			} catch (Exception e) {
				Log.e(TAG, "Failed to write tag");
				return 3;
			} finally {
				try {
					ndef.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return 2;
		} else {
			NdefFormatable ndefFormatable = NdefFormatable.get(tag);
			if(ndefFormatable != null) {
				try {
					ndefFormatable.connect();
					ndefFormatable.format(ndefMessage);
//					ndefFormatable.formatReadOnly(ndefMessage);
					Log.d(TAG, "Formatted tag and wrote message");
				} catch (Exception e) {
					Log.e(TAG, "Failed to format tag.");
					return 3;
				} finally {
					try {
						
						ndefFormatable.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				return 0;
			} else {
				Log.e(TAG, "Tag doesn\'t support NDEF.");
				return 3;
			}
		}
	}
}





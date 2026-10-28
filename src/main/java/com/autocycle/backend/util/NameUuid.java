package com.autocycle.backend.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class NameUuid {

	public static final UUID BRAND_NAMESPACE = UUID.fromString("12345678-1234-5678-1234-567812345678");
	public static final UUID PLASTIC_NAMESPACE = UUID.fromString("87654321-4321-8765-4321-876543218765");

	private NameUuid() {
	}

	public static String brandId(String name) {
		return v5(BRAND_NAMESPACE, normalize(name)).toString();
	}

	public static String plasticId(String type) {
		return v5(PLASTIC_NAMESPACE, normalize(type)).toString();
	}

	public static UUID v5(UUID namespace, String name) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			sha1.update(toBytes(namespace));
			sha1.update(name.getBytes(StandardCharsets.UTF_8));
			byte[] hash = sha1.digest();
			hash[6] = (byte) ((hash[6] & 0x0f) | 0x50);
			hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
			ByteBuffer buffer = ByteBuffer.wrap(hash);
			return new UUID(buffer.getLong(), buffer.getLong());
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static byte[] toBytes(UUID uuid) {
		ByteBuffer buffer = ByteBuffer.allocate(16);
		buffer.putLong(uuid.getMostSignificantBits());
		buffer.putLong(uuid.getLeastSignificantBits());
		return buffer.array();
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase();
	}
}

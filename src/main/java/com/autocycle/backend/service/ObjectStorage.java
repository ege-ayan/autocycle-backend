package com.autocycle.backend.service;

public interface ObjectStorage {

	String uploadPng(byte[] png, String productName);

	String upload(byte[] body, String folder, String name, String contentType, String extension);
}

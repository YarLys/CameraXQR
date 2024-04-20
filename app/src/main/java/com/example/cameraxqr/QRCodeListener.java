package com.example.cameraxqr;

public interface QRCodeListener {
    public void onQRFound(String qrcode);
    public void onQRNotFound();
}

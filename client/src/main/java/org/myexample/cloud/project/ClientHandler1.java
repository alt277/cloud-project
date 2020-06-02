package org.myexample.cloud.project;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;

public class ClientHandler1 extends ChannelInboundHandlerAdapter {
    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private static final byte SIGNAL_BYTE_MESSAGE = 20;
    private static final byte SIGNAL_BYTE_FILE = 25;
    private static final byte SYGNAL_AUTH_OK=15;

    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private byte controlByte;
    private BufferedOutputStream out;
    Channel channel;
    Path path;
    String command;

    // контекст  - вся информация о соединении с клиентом
    @Override                                // ссылка на контекст  +  посылка
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte readed = buf.readByte();
                if (readed == SIGNAL_BYTE_FILE|| readed==SIGNAL_BYTE_MESSAGE) {
                    controlByte=readed;
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
                    System.out.println("STATE: Start  receiving");
                }else if(readed==SYGNAL_AUTH_OK) {
                    System.out.println("Got Auth OK");
//                    controller.setAuthorized(true);
                    Authorisator.setAuthorised(true);
                }
                else {
                    System.out.println("ERROR: Invalid first byte - " + readed);
                }
            }
            if (controlByte == SIGNAL_BYTE_FILE) {
                if (currentState == State.NAME_LENGTH) {
                    if (buf.readableBytes() >= 4) {
                        System.out.println("STATE: Get name length");
                        nextLength = buf.readInt();
                        currentState = State.NAME;
                    }
                }
                if (currentState == State.NAME) {
                    if (buf.readableBytes() >= nextLength) {
                        byte[] fileName = new byte[nextLength];
                        buf.readBytes(fileName);
                        System.out.println("STATE: Filename received: " + new String(fileName, "UTF-8"));
                        out = new BufferedOutputStream(new FileOutputStream("client_storage/" + new String(fileName)));
                        currentState = State.FILE_LENGTH;
                    }
                }
                if (currentState == State.FILE_LENGTH) {
                    if (buf.readableBytes() >= 8) {
                        fileLength = buf.readLong();
                        System.out.println("STATE: File length received - " + fileLength);
                        currentState = State.FILE;
                    }
                }
                if (currentState == State.FILE) {
                    while (buf.readableBytes() > 0) {
                        out.write(buf.readByte());
                        receivedFileLength++;
                        if (fileLength == receivedFileLength) {
                            currentState = State.IDLE;
                            System.out.println("File received");
                            out.close();
                            break;
                        }
                    }
                }
            } else if (controlByte==SIGNAL_BYTE_MESSAGE){
                if (currentState == State.NAME_LENGTH) {
                    if (buf.readableBytes() >= 4) {
                        System.out.println("STATE message: Get filename length");
                        nextLength = buf.readInt();
                        currentState = State.NAME;
                    }
                }
                if (currentState == State.NAME) {
                    if (buf.readableBytes() >= nextLength) {
                        byte[] fileName = new byte[nextLength];
                        buf.readBytes(fileName);
                        ctx.fireChannelRead(fileName);  // толкаем дальше

                        System.out.println("InHandler: STATE: message received: " + new String(fileName, "UTF-8"));

                        currentState = State.IDLE;
                        break;
                    }
                }
            }
        }


        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace(); // обязательно делать токое переопределение чтобы знать
        ctx.close();             // что произошло
        System.out.println(" ошибки во входяшем хендлере клиента");
    }
}

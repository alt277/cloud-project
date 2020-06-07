package org.myexample.cloud.project.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Sender {

    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private static final byte SIGNAL_COMMAND =20;
    private static final byte SIGNAL_BYTE_FILE=25;
    private static final byte SYGNAL_BYTE_OK=15;
    private static final byte SYGNAL_REFRESH=10;

    private static final String GET_FILE= "GET?";
    private static String DELETE_FILE= "DEL?";
    private static String OPEN_ACCESS= "OPE?";
    private static String CLOSE_ASSESS= "CLO?";
    private static String SYNCHRONIZE= "SYN?";
    private static String AUTHORISE= "AUT?";
    private static String AUTHORIZATION_OK= "AUTOK?";
    private static String SERVER_LIST= "SERVLIST?";





    private State currentState = State.IDLE;
    private  int nextLength;
    private long fileLength;
    private  long receivedFileLength;

    private  BufferedOutputStream out;



    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {

        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));

        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(SIGNAL_BYTE_FILE);
        channel.writeAndFlush(buf);

        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(filenameBytes.length);
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
        buf.writeLong(Files.size(path));
        channel.writeAndFlush(buf);

        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public static void getFile(String filename, Channel channel, ChannelFutureListener finishListener) throws IOException {
        if (Files.notExists(Paths.get("client_storage/" + filename))) {
            String command = GET_FILE + filename;
            sendCommand(command, channel, finishListener);
        }
    }
    public static void deleteFile(String filename, Channel channel, ChannelFutureListener finishListener) throws IOException {
        String command=DELETE_FILE+ filename;
        sendCommand(command,channel,finishListener);
    }
    public static void openAccess(String filename, Channel channel, ChannelFutureListener finishListener) throws IOException {
        String command=OPEN_ACCESS+filename;
        sendCommand(command,channel,finishListener);
    }
    public static void closeAccess(String filename, Channel channel, ChannelFutureListener finishListener) throws IOException {
        String command=CLOSE_ASSESS+filename;
        sendCommand(command,channel,finishListener);
    }
    public static void authorizeCMD(String filename, Channel channel, ChannelFutureListener finishListener) throws IOException {
        String command=AUTHORISE+filename;
        sendCommand(command,channel,finishListener);
    }
    public static void authorizationOK(String way, Channel channel, ChannelFutureListener finishListener) throws IOException {
        String command=AUTHORIZATION_OK+way;
        sendCommand(command,channel,finishListener);
 //       System.out.println("Путь для обновления клиенту"+way);
    }
    public static void sendCommand(String command, Channel channel, ChannelFutureListener finishListener) throws IOException {
     //  String mes=command+path.getFileName().toString();
        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(SIGNAL_COMMAND);
        channel.writeAndFlush(buf);

        byte[] filenameBytes = command.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(filenameBytes.length);
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
//        channel.writeAndFlush(buf);
        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }
    public static void sendOK( Channel channel) throws IOException {

        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(SYGNAL_BYTE_OK);
        channel.writeAndFlush(buf);

    }
    public static void sendRefresh( Channel channel) throws IOException {

        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(SYGNAL_REFRESH);
        channel.writeAndFlush(buf);

    }
    public static void sendSYNC(List<String> filelist, Channel channel, ChannelFutureListener finishListener) throws IOException {
        System.out.println("Список клиента в методе СИНК : ");
     filelist.stream().forEach(o-> System.out.println(o));
     String stringOfFiles=new String(SYNCHRONIZE);
        for (String o : filelist) {
            stringOfFiles += o + "?";
        }
        System.out.println("Получилась команда со списком? : "+stringOfFiles);
        sendCommand(stringOfFiles,channel,finishListener);
    }
    public static void sendRefreshList(List<String> filelist, Channel channel, ChannelFutureListener finishListener) throws IOException {
        System.out.println("Список клиента в методе сервера sendRefreshList  : ");
        filelist.stream().forEach(o-> System.out.println(o));
        String stringOfFiles=new String(SERVER_LIST);
        for (String o : filelist) {
            stringOfFiles += o + "?";
        }
        System.out.println("Получилась команда со списком обновления с сервера : "+stringOfFiles);
        sendCommand(stringOfFiles,channel,finishListener);
    }
}
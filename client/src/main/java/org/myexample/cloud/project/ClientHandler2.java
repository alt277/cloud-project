package org.myexample.cloud.project;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.myexample.cloud.project.common.Sender;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler2 extends ChannelInboundHandlerAdapter {
    private   RefreshCallback refreshCallback;
    ClientHandler2 (RefreshCallback refreshCallback) {
        this.refreshCallback=refreshCallback;
    }

    private static final String SYNCHRONIZE = "SYN";
    private static String AUTHORIZATION_OK= "AUTOK?";
    private static String SERVER_LIST= "SERVLIST?";

    protected static List<String> refreshingFiles = new ArrayList<>();
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        byte[] arr = (byte[]) msg;
        String message = new String(arr);
        System.out.println("message= " + message);
        String[] parts = message.split("\\?");
        String fileName = "";
        for (int i = 1; i < parts.length; i++) {
            fileName += parts[i];
        }

        System.out.println("Имя файла на клиенте после удал. комманды:" + fileName);

        if (message.startsWith(SYNCHRONIZE)) {
            System.out.println("Полученный список файлов:  " + message);

            List<String> missingFiles = new ArrayList<>();

            for (int i = 1; i < parts.length; i++) {
                missingFiles.add(parts[i]);                // получаем список файлов клиента
            }
            missingFiles.stream().forEach(o -> System.out.println(o));

            for (String o : missingFiles) {
                System.out.println("Блок отсылки");
                if (Files.exists(Paths.get(ClientHandler1.storage_way + o))) {
                    Sender.sendFile(Paths.get(ClientHandler1.storage_way + o),
                            ctx.channel(), future -> {
                                if (!future.isSuccess()) {
                                    future.cause().printStackTrace();
                                }
                                if (future.isSuccess()) {
                                    System.out.println(" Отсутствующий файл пошел с клиента " + o);
                                }
                            });
                }
            }
            System.out.println("Missing files before CLEAR ");
            missingFiles.stream().forEach(o -> System.out.println(o));
            missingFiles.clear();

            System.out.println("Missing files after CLEAR= ");
            missingFiles.stream().forEach(o -> System.out.println(o));
            message="";
            System.out.println(" Message после очитски = " + message);

        }
        if (message.startsWith(AUTHORIZATION_OK)) {
            System.out.println("parts[1]= "+parts[1]);
            ClientHandler1.storage_way=parts[1];
            System.out.println("storage way= "+  ClientHandler1.storage_way);
            if (Files.notExists(Paths.get(parts[1]))) {
                System.out.println("Новая директория!");
                Files.createDirectory(Paths.get(parts[1]));
            }
//            refreshingFiles.clear();
//            for (int i = 1; i < parts.length; i++) {
//                refreshingFiles.add(parts[i]);
//            }
        }
        if (message.startsWith(SERVER_LIST)) {
            System.out.println("Полученный список файлов обновления  " + message);
                       //     List<String> refreshingFiles = new ArrayList<>();
          refreshingFiles.clear();
            for (int i = 1; i < parts.length; i++) {
                refreshingFiles.add(parts[i]);
            }
            refreshCallback.refresh();
            refreshingFiles.stream().forEach(o -> System.out.println(o));


            System.out.println("Refreshing files before CLEAR ");
            refreshingFiles.stream().forEach(o -> System.out.println(o));
       //     refreshingFiles.clear();

            System.out.println("Refreshing files after CLEAR= ");
            refreshingFiles.stream().forEach(o -> System.out.println(o));
            message="";
            System.out.println(" Message после очитски = " + message);


        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
        System.out.println(" ошибки при передаче в 2 хендлере");
    }
}
package org.myexample.cloud.project;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.myexample.cloud.project.common.Sender;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler2 extends ChannelInboundHandlerAdapter {


    private static final String SYNCHRONIZE = "SYN";


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

//            List<String> clientFiles = new ArrayList<>();
//            List<String> serverFiles = new ArrayList<>();
            List<String> missingFiles = new ArrayList<>();

            for (int i = 1; i < parts.length; i++) {
                missingFiles.add(parts[i]);                // получаем список файлов клиента
            }
            missingFiles.stream().forEach(o -> System.out.println(o));

            for (String o : missingFiles) {

                System.out.println("Блок отсылки");
                Sender.sendFile(Paths.get("client_storage/" + o),
                        ctx.channel(), future -> {

                            if (!future.isSuccess()) {
                                future.cause().printStackTrace();
                            }
                            if (future.isSuccess()) {
                                System.out.println(" Отсутствующий файл пошел с клиента " + o);
                            }
                        });
            }
            System.out.println("Missing files before CLEAR ");
            missingFiles.stream().forEach(o -> System.out.println(o));
            missingFiles.clear();

            System.out.println("Missing files after CLEAR= ");
            missingFiles.stream().forEach(o -> System.out.println(o));
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
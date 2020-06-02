package org.myexample.cloud.project;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.myexample.cloud.project.common.Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Handler2 extends ChannelInboundHandlerAdapter {
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private static final String GET_FILE = "GET";
    private static final String DELETE_FILE = "DEL";
    private static final String OPEN_ACCESS = "OPE";
    private static final String CLOSE_ASSESS = "CLO";
    private static final String SYNCHRONIZE = "SYN";
    private static String AUTHORISE = "AUT?";
    private HashMap<Channel,String> clientsList= new HashMap<>();

    private static final byte SYGNAL_AUTH_OK = 15;

    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

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
        System.out.println("message полученный = " +message);

        System.out.println("Имя файла после удал. комманды:" + fileName);
        if (message.startsWith(GET_FILE)) {
            if (Files.exists(Paths.get("server_storage/" + fileName))) {
                Sender.sendFile(Paths.get("server_storage/" + fileName),
                        ctx.channel(), future -> {

                            if (!future.isSuccess()) {
                                future.cause().printStackTrace();
                            }
                            if (future.isSuccess()) {
                                System.out.println(" Файл отослан с сервера " );
                            }
                        });
                System.out.println(" Handler2 : file:" + fileName + " отослан ");
            }
        }
        if (message.startsWith(DELETE_FILE)) {
            Files.deleteIfExists(Paths.get("server_storage/" + fileName));
            Files.deleteIfExists(Paths.get("Access_storage/" + fileName));
            System.out.println("Блок delete!");
        }
        if (message.startsWith(OPEN_ACCESS)) {
            System.out.println(" блок откр доступ ");
            if (Files.exists(Paths.get("server_storage/" + fileName))) {

                int size = (int) Files.size(Paths.get("server_storage/" + fileName));
                byte[] arr1 = new byte[size];
                in = new BufferedInputStream(new FileInputStream("server_storage/" + fileName));
                in.read(arr1);
                out = new BufferedOutputStream(new FileOutputStream("Access_storage/" + fileName));
                out.write(arr1);
                in.close();
                out.close();

                //     Files.write(Paths.get("server_storage/" + fileName)), ;
                //    FileRegion region = new DefaultFileRegion(Paths.get("server_storage/" + fileName), 0, Files.size(path));
                //   out.write(region);
                System.out.println(" блок откр доступ ");
            }
        }

        if (message.startsWith(CLOSE_ASSESS)) {
            System.out.println(" блок закр доступ ");
            if (Files.exists(Paths.get("Access_storage/" + fileName))) {
                Files.delete(Paths.get("Access_storage/" + fileName));
                System.out.println(" блок закр доступ ");
            }
            Files.deleteIfExists(Paths.get("Access_storage/" + fileName));
        }

        if (message.startsWith(AUTHORISE)) {
            authService = new BaseAuthService();
            System.out.println("сообщение с логином и паролем =  " + message);
            String[] mass = message.split("\\?");

            String nick =
                    authService.getNickByLoginPass(mass[1], mass[2]);
            if (nick != null) {
                Sender.sendOK(ctx.channel());
                System.out.println("Успешная авторизация клиента " + nick);
                String storageName=nick+"_storage/";
                clientsList.put(ctx.channel(),nick);
                Files.createDirectory(Paths.get(storageName));
          //      out = new BufferedOutputStream(new FileOutputStream(storageName));

            }
        } else {
            System.out.println("Неверные логин/пароль");
        }

        if (message.startsWith(SYNCHRONIZE)) {
            System.out.println("Полученный список файлов:  " + message);

            List<String> clientFiles = new ArrayList<>();
            List<String> serverFiles = new ArrayList<>();
            List<String> missingFiles = new ArrayList<>();

            for (int i = 1; i < parts.length; i++) {
                clientFiles.add(parts[i]);                // получаем список файлов клиента
            }
            clientFiles.stream().forEach(o -> System.out.println(o));

            Files.list(Paths.get("server_storage"))
                    .filter(p -> !Files.isDirectory(p))
                    .map(p -> p.getFileName().toString())
                    .forEach(o -> serverFiles.add(o));     // получаем список файлов сервера

            System.out.println("список файлов сервера: ");
            serverFiles.stream().forEach(o -> System.out.print(o+" "));

            for (String o : serverFiles) {
                if (!(clientFiles.contains(o))) {
                    System.out.println("Блок отсылки");
                    Sender.sendFile(Paths.get("server_storage/" + o),
                            ctx.channel(), future -> {

                                if (!future.isSuccess()) {
                                    future.cause().printStackTrace();
                                }
                                if (future.isSuccess()) {
                                    System.out.println(" Отсутствующий файл пошел с сервера "+o);
                                }
                            });
                }
            }
            for (String o : clientFiles) {
                if (!(serverFiles.contains(o))) {
                    missingFiles.add(o);
                }
            }
            Sender.sendSYNC(missingFiles,
                    ctx.channel(), future -> {

                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            System.out.println(" Список осутствующих файлов пошел с сервера ");
                        }
                    });
            System.out.println("Список осутствуюших на сервере файлов: ");
            missingFiles.stream().forEach(o -> System.out.print(o+" "));
//                clientFiles.clear();
//                serverFiles.clear();
//                missingFiles.clear();
//                message="";
//            System.out.println("message после очистки = " +message);

//            System.out.println("Списки после очистки: ");
//                clientFiles.stream().forEach(o -> System.out.print(o+" "));
//                serverFiles.stream().forEach(o -> System.out.print(o+" "));
//                missingFiles.stream().forEach(o -> System.out.print(o+" "));
//            System.out.println("Конец списков после очистки: ");
        }


    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
        System.out.println(" ошибки при передаче в 2 хендлере");
    }


}


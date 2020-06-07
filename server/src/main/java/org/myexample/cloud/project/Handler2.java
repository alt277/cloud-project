package org.myexample.cloud.project;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.myexample.cloud.project.common.Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Handler2 extends ChannelInboundHandlerAdapter {

    protected  List<String> clientFiles = new ArrayList<>();
    protected  List<String> serverFiles = new ArrayList<>();
    protected  List<String> missingFiles = new ArrayList<>();

    private BufferedInputStream in;
    private BufferedOutputStream out;
    private static final String GET_FILE = "GET";
    private static final String DELETE_FILE = "DEL";
    private static final String OPEN_ACCESS = "OPE";
    private static final String CLOSE_ASSESS = "CLO";
    private static final String SYNCHRONIZE = "SYN";
    private static String AUTHORISE = "AUT?";
 //   private HashMap<Channel,String> clientsList= new HashMap<>();

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

        System.out.println("Имя файла после удал. комманды:"
                + fileName);
        if (message.startsWith(GET_FILE)) {

            String storageName=Clients.list.get(ctx.channel())+"_server_storage/";
            System.out.println("storageName="+storageName);
            if (Files.exists(Paths.get(storageName+fileName))) {

                Sender.sendFile(Paths.get(storageName + fileName),
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
            if(!(Clients.list.get(ctx.channel())==null));
            {
                Clients.sendListOfFilesToRefresh( Clients.list.get(ctx.channel()) + "_server_storage/", ctx.channel());
            }
        }
        if (message.startsWith(DELETE_FILE)) {
            String storageName=Clients.list.get(ctx.channel())+"_server_storage/";
            Files.deleteIfExists(Paths.get(storageName + fileName));
            Files.deleteIfExists(Paths.get("Access_storage/" + fileName));
            System.out.println("Блок delete!");
        }
        if (message.startsWith(OPEN_ACCESS)) {
            System.out.println(" блок откр доступ ");
            String storageName=Clients.list.get(ctx.channel())+"_server_storage/";
            System.out.println("storageName="+storageName);
            if (Files.exists(Paths.get(storageName+fileName))) {

                out = new BufferedOutputStream(new FileOutputStream("Access_storage/" + fileName));
                Files.copy(Paths.get("Access_storage/" + fileName),out);
                out.close();
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
            if ( (nick != null)  && (!Clients.isNickBusy(nick)) ) {
 //               Sender.sendOK(ctx.channel());
                System.out.println("Успешная авторизация клиента " + nick);
                String storageName=nick+"_server_storage/";
                String clientStorageName="client_"+nick+"_storage/";
                Clients.list.put(ctx.channel(),nick);
                Sender.authorizationOK(clientStorageName,ctx.channel(), future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        System.out.println("Успешная авторизация клиента " + nick+
                                " хранилище клиента : "+clientStorageName);
                    }
                });;
                if (Files.notExists(Paths.get(storageName))) {
                    System.out.println("Новая директория!");
                    Files.createDirectory(Paths.get(storageName));

                }
                if(!(Clients.list.get(ctx.channel())==null));
                {
                    Clients.sendListOfFilesToRefresh( Clients.list.get(ctx.channel()) + "_server_storage/", ctx.channel());
                }
                for (Map.Entry<Channel,String> e : Clients.list.entrySet()) {      // это -   Set <Map.Entry<K,V>>entrySet()
                    Channel key = e.getKey();
                    String value = e.getValue();
                    System.out.println("Channel="+key);
                    System.out.println("Nick ="+value);
                    }
                Sender.sendOK(ctx.channel());

            } else if(Clients.isNickBusy(nick)){
                      System.out.println("Учетная запись уже использется");
            }else{
                      System.out.println(" Неверный логин или пароль ");
                 }
  //          Clients.sendListOfFilesToRefresh(serverFiles, Clients.list.get(ctx.channel())+"_server_storage/" , ctx.channel() );
        }

        if (message.startsWith(SYNCHRONIZE)) {
            System.out.println("Полученный список файлов:  " + message);

            for (int i = 1; i < parts.length; i++) {
                clientFiles.add(parts[i]);                // получаем список файлов клиента
            }
            clientFiles.stream().forEach(o -> System.out.println(o));
            String way = Clients.list.get(ctx.channel())+"_server_storage/";
            Files.list(Paths.get(way))
                    .filter(p -> !Files.isDirectory(p))
                    .map(p -> p.getFileName().toString())
                    .forEach(o -> serverFiles.add(o));     // получаем список файлов сервера

            System.out.println("список файлов сервера: ");
            serverFiles.stream().forEach(o -> System.out.print(o+" "));

            for (String o : serverFiles) {
                if (!(clientFiles.contains(o))) {
                    System.out.println("Блок отсылки");
                    Sender.sendFile(Paths.get(way + o),
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
 //           Sender.sendRefresh(ctx.channel());

        }
      //  Sender.sendRefresh(ctx.channel());
        if(!(Clients.list.get(ctx.channel())==null));{
            Clients.sendListOfFilesToRefresh(Clients.list.get(ctx.channel()) + "_server_storage/", ctx.channel());
            System.out.println("Отсылка обновления в конце метода ChannelRead сработала");
        }
        clientFiles.clear();
        serverFiles.clear();
        missingFiles.clear();

    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
        System.out.println(" ошибки при передаче в 2 хендлере");
    }


}


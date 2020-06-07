package org.myexample.cloud.project;

import io.netty.channel.Channel;
import org.myexample.cloud.project.common.Sender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Clients {
  protected   static HashMap<Channel,String>  list= new HashMap<>();
  public static synchronized boolean isNickBusy(String nick) {
    for (Map.Entry<Channel,String> o : list.entrySet()) {
      if (o.getValue().equals(nick)) {
        return true ;
      }
    }
    return false ;
  }
 // public static void sendListOfFilesToRefresh (List serverFiles, String way, Channel channel) throws IOException {
 public static void sendListOfFilesToRefresh (String way, Channel channel) throws IOException {

    List<String> serverFiles = new ArrayList<>();
    Files.list(Paths.get(way))
            .filter(p -> !Files.isDirectory(p))
            .map(p -> p.getFileName().toString())
            .forEach(o -> serverFiles.add(o));     // получаем список файлов сервера

          System.out.println("список файлов клиента на сервере: ");
          serverFiles.stream().forEach(o -> System.out.print(o+" "));

    Sender.sendRefreshList(serverFiles,channel, future -> {
      if (!future.isSuccess()) {
        future.cause().printStackTrace();
      }
      if (future.isSuccess()) {
        System.out.println("Список файлов для обновления передан ");

      }
    });
  // serverFiles.clear();
  }

}

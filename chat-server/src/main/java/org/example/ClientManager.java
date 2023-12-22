package org.example;

import java.io.*;
import java.net.Socket;

public class ClientManager implements Runnable {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;

    public ClientManager(Socket socket) {
        try {
            this.socket = socket;
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            ClientManagerSingleton.getInstance().add(this);
            System.out.println(name + " подключился к чату!");
            broadcastMessage("Server: " + name + " подключился к чату.");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    /**
     * Завершение работы всех потоков, закрытие соединения с клиентским сокетом,
     * удаление клиентского сокета из коллекции
     *
     * @param socket         клиентский сокет
     * @param bufferedReader буфер для чтения данных
     * @param bufferedWriter буфер для отправки данных
     */
    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с кдиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаление клиента из коллекции
     */
    private void removeClient() {
        ClientManagerSingleton.getInstance().remove(this);
        System.out.println(name + " покинул чат.");
    }

    @Override
    public void run() {
        String messageFromClient;

        // Цикл чтения данных от клиента
        while (socket.isConnected()) {
            try {
                // чтение данных
                messageFromClient = bufferedReader.readLine();
                broadcastMessage(messageFromClient);

            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    /**
     * Отправка сообщения всем слушателям
     *
     * @param message сообщение
     */
    private void broadcastMessage(String message) {
        String firstName = checkName(message);  // проверяем через метод "checkName" есть ли отсылка к приватному сообщению через знак '$'
        for (ClientManager client : ClientManagerSingleton.getInstance()) {
            try {
                // если отсылка к приватному сообщению есть, то направляем сообщение ТОЛЬКО одному адресату
                if (firstName != null) {
                    if (client.name.equals(firstName)) {
                        String name = message.split(": ", 2)[0];
                        String mess = message.split(": ", 2)[1].substring(firstName.length() + 1);

                        client.bufferedWriter.write(name + ":" + mess);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    }
                    // Если отсылки на приватное сообщение нет
                } else
                    // Если клиент не равен по наименованию клиенту-отправителю, отправим сообщение
                    if (!client.name.equals(name)) {
                        client.bufferedWriter.write(message);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    // Метод, определяющий наличие отсылки к приватному сообщению
    private String checkName(String message) {
        String nameAfter$ = null;
        String firstWord = message.split(": ", 2)[1];
        if (firstWord.startsWith("$")) {
            String textAfter$ = firstWord.split(" ", 2)[0].substring(1);
            for (ClientManager client : ClientManagerSingleton.getInstance()) {
                if (client.name.equals(textAfter$)) nameAfter$ = textAfter$;
            }
        }
        return nameAfter$;
    }
}

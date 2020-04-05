package red.mohist.down;

import red.mohist.configuration.MohistConfigUtil;
import red.mohist.util.JarLoader;
import red.mohist.util.i18n.Message;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

public class DownloadLibraries {
    static boolean needToRecheck = false;
    static int retry = 0;
    static FileChannel fileChannel;

    public static void run() throws Exception {
        System.out.println(Message.getString("libraries.checking.start"));
        String str;
        String url = "https://www.mgazul.cn/";
        if(Message.getLocale().contains("CN") || Message.getCountry().contains("CN"))
            url = "https://mohist-community.gitee.io/mohistdown/"; //Gitee Mirror

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(DownloadLibraries.class.getClassLoader().getResourceAsStream("lib.red")));
            while ((str = bufferedReader.readLine()) != null) {
                String[] args = str.split("\\|");
                if(args.length == 2) {
                    File file = new File(args[0]); // Judgement files and MD5
                    if((!file.exists() || !DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file.toPath()))).toLowerCase().equals(args[1])) && !MohistConfigUtil.getTranslation(new File("mohist-config", "mohist.yml"), "libraries_black_list:", "xxxxx").contains(file.getName())) {
                        file.getParentFile().mkdirs();

                        URLConnection conn = new URL(url + args[0]).openConnection();
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
                        conn.connect();

                        System.out.println(Message.getFormatString("file.download.start", new Object[]{url + args[0], Update.getSize(conn.getContentLength())}));
                        try {
                            fileChannel = FileChannel.open(Paths.get(file.getAbsolutePath()), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                            fileChannel.transferFrom(Channels.newChannel(conn.getInputStream()), 0L, Long.MAX_VALUE);
                        } catch (Exception e) {
                            System.out.println(Message.getFormatString("file.download.nook", new Object[]{url + args[0]}));
                            needToRecheck = true;
                            retry++;
                        }
                        System.out.println(Message.getFormatString("file.download.ok", new Object[]{file.getName()}));
                        JarLoader.loadjar(new JarLoader((URLClassLoader) ClassLoader.getSystemClassLoader()), file.getParent());
                    }
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        if(needToRecheck && retry < 3) {
            needToRecheck = false;
            System.out.println(Message.getFormatString("update.retry", new Object[]{retry}));
            run();
        } else {
            if(fileChannel != null)
                fileChannel.close();
            System.out.println(Message.getString("libraries.checking.end"));
        }
    }

    public static boolean isCheck() { return MohistConfigUtil.getBoolean(new File("mohist-config", "mohist.yml"), "check_libraries:"); }
}

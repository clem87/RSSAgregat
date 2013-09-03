/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.utils;

import java.io.File;
import java.util.Properties;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Une classe posédant des méthode static dédiée au maniement de fichiers
 * properties.
 *
 * @author clem
 */
public class PropertyLoader {

    /**
     * Charge le fichier properties QUI EST CONTENU DANS LE PROJET
     *
     * @param filename le fichier contenant les propriétés
     * @return un objet Properties contenant les propriétés du fichier
     */
    public static Properties load(String filename) throws IOException, FileNotFoundException {
        Properties properties = new Properties();
        InputStream is = PropertyLoader.class.getClassLoader().getResourceAsStream(filename);
        properties.load(is);
        Properties p = new Properties();
        is.close();
        return properties;
    }

    /**
     * *
     * Charge un fichier properties depuis un emplacement sur le disque dur
     * (exemple : /var/lib/RSSAgregate/conf.properties)
     *
     * @param filename : ler path et nom de fichier exemple
     * "/var/lib/RSSAgregate/conf.properties"
     * @return L'objet properties ou null si il n'a pas été trouvé"
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Properties loadFromFile(String filename) {
        FileInputStream fis = null;
        Properties p=null;
        try {
             p = new Properties();
             fis = new FileInputStream(filename);
            p.load(fis);

        } catch (Exception e) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    Logger.getLogger(PropertyLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return p;
    }

    /**
     * *
     * Sauvegarde l'objet properties dans un fichier properties propre au projet
     * NE MARCHE PAS VRAIMENT, après redémarage on ne retrouve pas les valeur et
     * le fichier est retourné a son état initiale. On préfère utiliser la
     * méthode saveToFile pour sauvegarder dans un répertoire a part / usr/lib.
     * Le fichier conf.properties notamment
     *
     * @param prop
     * @param filename
     * @param comment
     * @throws URISyntaxException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void save(Properties prop, String filename, String comment) throws URISyntaxException, FileNotFoundException, IOException {

        URL resourceUrl = PropertyLoader.class.getClassLoader().getResource(filename);
        System.out.println("URL DU FICHIER PROP : " + resourceUrl.toURI());
//        URL truc = PropertyLoader.class.getClassLoader().getResourceAsStream(filename).;

        File file = new File(resourceUrl.toURI());
        System.out.println("LA PATH : " + file.getAbsolutePath());
        OutputStream os = new FileOutputStream(file);
        prop.store(os, "lalala");
        os.close();
    }

    /**
     * *
     * Sauvegarde l'objet properties dans un fichier properties correspondant
     * aux références envoyées en paramettre
     *
     * @param prop : l'objet properties à sauvegarder
     * @param fileDestination : le fichier destination
     */
    public static void saveToFile(Properties prop, String fileDestination, String comment) throws FileNotFoundException, IOException {

        OutputStreamWriter osw = new FileWriter(fileDestination);
        prop.store(osw, comment);

    }

    /**
     * *
     * Charge une propriété a partir du fichier properties demandé
     *
     * @param filename
     * @param prop
     * @return
     * @throws IOException
     */
    public static String loadProperti(String filename, String prop) throws IOException {
        Properties p = load(filename);
        String sp = p.getProperty(prop);
        return sp;
    }

    /**
     * *
     * Un main pour tester
     *
     * @param args
     */
    public static void main(String[] args) {

        Properties pp = new Properties();
        pp.setProperty("#C'est mon commentaire ", "");

        pp.setProperty("name", "Bobizz");
        try {
            OutputStreamWriter osw = new FileWriter("/var/lib/RSSAgregate/conf.properties");

            pp.store(osw, null);
        } catch (IOException ex) {
            Logger.getLogger(PropertyLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

        Properties p = new Properties();
        FileInputStream fis;
        try {
            fis = new FileInputStream("/var/lib/RSSAgregate/conf.properties");
            p.load(fis);
            System.out.println("name : " + p.getProperty("name"));

        } catch (FileNotFoundException ex) {
            Logger.getLogger(PropertyLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PropertyLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
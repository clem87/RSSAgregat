/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import rssagregator.services.ListeFluxCollecteEtConfigConrante;
import rssagregator.services.ServiceCollecteur;

/**
 *
 * @author clem
 */
public class StartServlet implements ServletContextListener {

    private static final String ATT_LIST_FLUX = "listflux";
    private ListeFluxCollecteEtConfigConrante listflux;
    private static final String ATT_SERVICE_COLLECTE = "collecte";
    private ServiceCollecteur collecte;

    /**
     * *
     * Méthode lancée au démarrage de l'application. permet d'initialiser les
     * différents services
     *
     * @param sce
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Class.forName("com.mysql.jdbc.Driver");


            // Initialisation de la liste des flux.
            listflux = ListeFluxCollecteEtConfigConrante.getInstance();

            // Initialisation du  collecteur
            collecte = ServiceCollecteur.getInstance();

            // On enregistre les service comme observer et la liste des flux comme observable
            listflux.addObserver(collecte);

            // On charge la liste des flux depuis la base de donnée
            listflux.chargerDepuisBd();

            listflux.forceChange();
            //        listflux.chargerDepuisBd();
            listflux.notifyObservers();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(StartServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // On arrete les taches de collecte

        collecte.stopCollecte();

        destroyDriver();
    }

    public void destroyDriver() {
        String prefix = getClass().getSimpleName() + " destroy() ";


        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                DriverManager.deregisterDriver(drivers.nextElement());
            }
        } catch (Exception e) {

            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception caught while deregistering JDBC drivers");
//        ctx.log(prefix + "Exception caught while deregistering JDBC drivers", e);
        }

    }
}

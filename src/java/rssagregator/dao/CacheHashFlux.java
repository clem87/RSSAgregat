/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rssagregator.beans.Flux;
import rssagregator.services.ServiceCollecteur;

/**
 * Un singleton permettant de concerver les hash des flux. Il est utilisé par la ou les dao flux ou item
 *
 * @author clem
 */
public class CacheHashFlux {

    static CacheHashFlux instance = new CacheHashFlux();
    Map<Flux, Set<String>> cacheHash = new HashMap<Flux, Set<String>>();

    public static CacheHashFlux getInstance() {
        if (instance == null) {
            instance = new CacheHashFlux();
        }
        return instance;
    }

    /**
     * *
     * Cette méthode doit être lancée au démarrage du {@link ServiceCollecteur}. Elle permet d'initialiser le cache des
     * flux avec les données récupérée dans la base de données
     */
    public void ChargerLesHashdesFluxdepuisBDD() {

        DaoFlux daoFlux = DAOFactory.getInstance().getDAOFlux();
        List<Flux> listflux = daoFlux.findAllFlux(Boolean.TRUE);
        DaoItem daoItem = DAOFactory.getInstance().getDaoItem();
        for (int i = 0; i < listflux.size(); i++) {
            Flux flux = listflux.get(i);
            Set<String> listHash = daoItem.findLastHash(flux, 500, false);
            addAll(flux, listHash);
        }

    }

    /**
     * *
     * Retourne la liste des hash concervés dans le cache pour un flux donnée. Le flux est retrouvé par comparaison de
     * sa clé primaire
     *
     * @param flux Le flux pour lequel il faut récupérer
     * @return Le set des hash. retour null si le n'a pas été trouvé dans le cache
     */
    public synchronized Set<String> returnLashHash(Flux flux) {
        if (flux != null && flux.getID() != null) {
            for (Map.Entry<Flux, Set<String>> entry : cacheHash.entrySet()) {
                Flux flux1 = entry.getKey();
                if (flux1.getID().equals(flux.getID())) {
                    Set<String> string = entry.getValue();
                    return string;
                }
            }
        }
        return null;
    }

    /**
     * *
     * Ajoute un hash en mémoire pour le flux donné en paramètre. Si le flux n'est pas trouvé, il est alors ajouté.
     *
     * @param flux Le flux pour lequel il faut ajouter le hash
     * @param hash Le hash a ajouter. L'ajout ne se fera que si le hash n'est pas null ou empty
     */
    public synchronized void addHash(Flux flux, String hash) {
        if (flux != null && hash != null && flux.getID() != null && flux.getID() > 0 && !hash.isEmpty()) {
            boolean trouve = false;
            for (Map.Entry<Flux, Set<String>> entry : cacheHash.entrySet()) {
                Flux flux1 = entry.getKey();

                if (flux1.getID().equals(flux.getID())) {
                    Set<String> string = entry.getValue();
                    trouve = true;
                    string.add(hash);
                }
            }
            //Si on n'a pas trouvé le flux, il faut l'ajouter
            if (!trouve) {
                Set<String> newHash = new HashSet<String>();
                newHash.add(hash);
                cacheHash.put(flux, newHash);
            }
        }
    }

    /**
     * *
     * Ajoute les hash envoyé en paramètre pour le flux donnée
     *
     * @param flux : Le flux pour lequel il faut ajouté les hash
     * @param setHash Les hash a ajouter
     */
    public void addAll(Flux flux, Set<String> setHash) {

        if (flux != null && setHash != null && setHash.size() > 0) {
            // On vérifi chacune des valeurs du set On retir ce qui pourrait être null ou empty
            for (Iterator<String> it = setHash.iterator(); it.hasNext();) {
                String string = it.next();
                if (string == null) {
                    it.remove();
                }
                if (string != null && string.isEmpty()) {
                    it.remove();
                }
            }

            boolean trouve = false;
            for (Map.Entry<Flux, Set<String>> entry : cacheHash.entrySet()) {
                Flux flux1 = entry.getKey();
                Set<String> key = entry.getValue();
                if (flux1.getID().equals(flux.getID())) {
                    trouve = true;
                    key.addAll(setHash);
                }
            }
            if (!trouve) {
                this.cacheHash.put(flux, setHash);
            }
        }
    }

    /**
     * *
     * Supprimer le hash envoyé en paramettre pour le flux envoyé
     *
     * @param flux le flux pour lequel il faut supprimer le hash
     * @param hash Le hash qu'il faut supprimer
     * @return
     */
    public synchronized Boolean reomveHash(Flux flux, String hash) {
        for (Map.Entry<Flux, Set<String>> entry : cacheHash.entrySet()) {
            Flux flux1 = entry.getKey();
            Set<String> set = entry.getValue();
            if (flux1.getID().equals(flux.getID())) {
                set.remove(hash);
            }
        }
        return false;
    }

    /**
     * *
     * Supprime le flux donnée du cache
     *
     * @param flux
     * @return true si le flux a été trouvé et supprimé. False si le flux n'a pas été trouvé
     */
    public synchronized Boolean removeFlux(Flux flux) {
        if (flux != null && flux.getID() != null && flux.getID() >= 0) {
            System.out.println("IF");
            
            for (Map.Entry<Flux, Set<String>> entry : cacheHash.entrySet()) {
                System.out.println("FOR");
                Flux flux1 = entry.getKey();
                                Set<String> set = entry.getValue();
                if(flux1.getID().equals(flux.getID())){
                     try {
                    System.out.println("try");
                    cacheHash.remove(flux);
                    return true;
                } catch (Exception e) {
                    return false;
                }
                }

                
            }
            if (cacheHash.containsKey(flux)) {
                System.out.println("Contain");
               
            }
        }
        return false;
    }

    public static void main(String[] args) {
    }
    
    
}

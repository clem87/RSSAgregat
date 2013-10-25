/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.beans.form;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.servlet.http.HttpServletRequest;
import rssagregator.beans.Journal;
import rssagregator.dao.DAOFactory;
import rssagregator.dao.DaoJournal;

/**
 * Classe permettant de valider et binder les données issues d'une requête dans un bean <strong>Journal</strong>
 *
 * @author clem
 */
public class JournalForm extends AbstrForm {
    //--------------------------------------
    // Les variables devant être récupérées

    private String nom;
    private String urlAccueil;
    private String urlHtmlRecapFlux;
    private String langue;
    private String pays;
    private String fuseauHorraire;
    private String information;
    private String typeJournal;
    //--------------------------------------
    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JournalForm.class);

    public JournalForm() {
        super();
    }

    @Override
    public Object bind(HttpServletRequest request, Object objEntre, Class type) {
//        return super.bind(request, objEntre, type); //To change body of generated methods, choose Tools | Templates.

        //Instanciation du journal si l'objet envoyé en argument est null
        if (this.action.equals("add")) {
            try {
                objEntre = type.newInstance();
//                objEntre =  type.newInstance();
            } catch (InstantiationException ex) {


                Logger.getLogger(JournalForm.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(JournalForm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Journal journal = (Journal) objEntre;

        //---------------------------------------------------
        // Bind des valeurs
        journal.setNom(nom);
        journal.setUrlAccueil(urlAccueil);
        journal.setUrlHtmlRecapFlux(urlHtmlRecapFlux);
        journal.setLangue(langue);
        journal.setPays(pays);
        journal.setFuseauHorraire(fuseauHorraire);
        journal.setInformation(information);
        journal.setTypeJournal(typeJournal);
        //---------------------------------------------------
        return journal;
    }

    @Deprecated
    public void check_nom(String nom) throws Exception {
        if (nom == null || nom.length() == 0) {
            throw new Exception("Ne peut être null");
        }
    }

    @Deprecated
    public void check_langue(String nom) throws Exception {
        if (nom == null || nom.length() == 0) {
            throw new Exception("Ne peut être null");
        }
    }

    @Override
    public Boolean validate(HttpServletRequest request) {
        //Bind du nom
        String s = request.getParameter("nom");
        if (s != null && !s.isEmpty()) {
            nom = s.trim();
            // On doit chercher si il s'existe pas déjà un journal avec ce nom
            DaoJournal dao = DAOFactory.getInstance().getDaoJournal();

            if (action.equals("add")) {
                Journal j = null;
                try {
                    j = dao.findWithName(nom);
                } catch (NonUniqueResultException e) {
                    logger.error("Plusieurs journaux portent le même nom. Ceci ne devrait jamais arriver !");
                    j = new Journal(); // C'est un problème on va instancier un journal pour bloquer l'ajout
                } 
                catch (NoResultException e){
                    logger.debug("pas de résult OK");
                }
                catch (Exception e) {
                    logger.error("Problème lors de l'usage de la dao : " + e);
                    j = new Journal(); // C'est un problème on va instancier un journal pour bloquer l'ajout
                }
                if (j != null) {
                    erreurs.put("nom", new String[]{"Il existe déjà un journal portant ce nom dans la base de données", "Il existe déjà un journal portant ce nom dans la base de données"});
                }
            }
        } else {
            erreurs.put("nom", new String[]{ERR_NE_PEUT_ETRE_NULL, ERR_NE_PEUT_ETRE_NULL});
        }


        s = request.getParameter("urlAccueil");
        if (s != null && !s.isEmpty()) {
            System.out.println("111");
            if (!s.matches(REG_EXP_HTTP_URL)) {
                erreurs.put("urlAccueil", new String[]{ERR_URL_INCORRECTE, ERR_URL_INCORRECTE});
            }
            urlAccueil = s;
        } else {
            erreurs.put("urlAccueil", new String[]{ERR_NE_PEUT_ETRE_NULL, ERR_NE_PEUT_ETRE_NULL});
        }

        s = request.getParameter("urlHtmlRecapFlux");
        if (s != null && !s.isEmpty()) {
            if (!s.matches(REG_EXP_HTTP_URL)) {
                erreurs.put("urlHtmlRecapFlux", new String[]{ERR_URL_INCORRECTE, ERR_URL_INCORRECTE});
            }
            urlHtmlRecapFlux = s;
        }

        s = request.getParameter("langue");
        if (s != null && !s.isEmpty()) {
            langue = s;
        }

        s = request.getParameter("pays");
        if (s != null && !s.isEmpty()) {
            pays = s;
        }

        s = request.getParameter("fuseauHorraire");
        if (s != null && !s.isEmpty()) {
            fuseauHorraire = s;
        }

        s = request.getParameter("information");
        if (s != null && !s.isEmpty()) {
            information = s;
        }


        s = request.getParameter("typeJournal");
        if (s != null && !s.isEmpty()) {
            if (s.equals("quotidien") || s.equals("hebdomadaire") || s.equals("mensuel") || s.equals("pure-player") || s.equals("autre")) {
                this.typeJournal = s;
            } else {
                erreurs.put("typeJournal", new String[]{"Valeur incorrecte", "Valeur incorrecte"});
            }
        } else {
            erreurs.put("typeJournal", new String[]{ERR_NE_PEUT_ETRE_NULL, ERR_NE_PEUT_ETRE_NULL});
        }

        valide = erreurs.isEmpty();
        return valide;
    }
}

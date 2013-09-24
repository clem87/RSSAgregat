/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.dao;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import rssagregator.beans.BeanSynchronise;
import rssagregator.beans.Flux;
import rssagregator.services.ServiceSynchro;

/**
 * Les DAO étende observable car certaine (flux, conf), sont enregistrée auprès
 * du service de collecte des flux par le patterne observateur
 *
 * @author clem
 */
public abstract class AbstrDao {

    protected EntityManager em;
    protected EntityManagerFactory emf;
    protected String PERSISTENCE_UNIT_NAME = "RSSAgregatePU2";
    protected DAOFactory dAOFactory;
//    protected static String REQ_FIND_ALL = "SELECT zazaza";
    protected Class classAssocie;
    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AbstrDao.class);

    public void creer(Object obj) throws Exception {
        logger.debug("usage de la DAO ! ===");
        //Il faut initialiser le em
//        em = dAOFactory.getEntityManager();
        EntityTransaction tr = em.getTransaction();
        tr.begin();
        em.persist(obj);
        try {
            if (BeanSynchronise.class.isAssignableFrom(obj.getClass())) {
                ServiceSynchro.getInstance().diffuser(obj, "add");
            }
            tr.commit();
        } catch (Exception e) {
            logger.error("Echec de la suppression du beans : " + e);
            tr.rollback();
            throw e;
        }

//        em.getTransaction().begin();
//        em.getTransaction().commit();
//        em.close();
    }

    public void modifier(Object obj) throws Exception {


        if (obj instanceof Flux) {
            Flux ff = (Flux) obj;
            System.out.println("FLUX DEBUT Abstr DE DAO : " + ff.getUrl());
        }

        // Test si le flux possède bien un id
        // On récupère l'id
        Method getter = obj.getClass().getMethod("getID");
        Object retour = getter.invoke(obj);

        if (retour != null && retour instanceof Long && (Long) retour >= 0) {
//            em = dAOFactory.getEntityManager();

            EntityTransaction tr = em.getTransaction();
//            System.out.println("ON MODIF");
            tr.begin();
//            em.getTransaction().begin();

            if (obj instanceof Flux) {
                Flux ff = (Flux) obj;
                System.out.println("FLUX AVANT  MERGE Abstr DE DAO : " + ff.getUrl());
            }


            System.out.println("em.contain : "+ em.contains(obj));           

            em.merge(obj); 
            
           

            if (obj instanceof Flux) {
                Flux ff = (Flux) obj;
                System.out.println("FLUX MERGE Abstr DE DAO : " + ff.getUrl());
            }


            try {
                // Si il s'agit d'un beans devant être synchronisé On lance la diff
                if (BeanSynchronise.class.isAssignableFrom(obj.getClass())) {
                    ServiceSynchro.getInstance().diffuser(obj, "mod");
                }
                tr.commit();

            } catch (Exception e) {
                // SI il y a une erreur lors du comit ou de la diffusion JMS On rollback
                logger.error("erreur lors de la modification du beans : " + e);
//                em.getTransaction().rollback();
                tr.rollback();
                throw e;
            }
        }

        if (obj instanceof Flux) {
            Flux ff = (Flux) obj;
            System.out.println("FLUX FIN DE DAO : " + ff.getUrl());
        }
    }

    /**
     * *
     * Retrouver un objet à patir de son id.
     *
     * @param id
     * @return
     */
    public Object find(Long id) {
//        em = dAOFactory.getEntityManager();
        Class laclass = this.getClassAssocie();
        System.out.println("LA CLASS : " + laclass);
        try {
            Object resu = em.find(laclass, id);
            return resu;
        } catch (Exception e) {
        }
//        em.getTransaction().commit();
//        em.close();
        return null;
    }

    /**
     * *
     * Supprimer le un objet Infocollecte...)
     *
     * @param obj
     */
    public void remove(Object obj) throws Exception {
//        em = dAOFactory.getEntityManager();
        EntityTransaction tr = em.getTransaction();
        tr.begin();
        em.remove(em.merge(obj));

        try {
            if (BeanSynchronise.class.isAssignableFrom(obj.getClass())) {
                ServiceSynchro.getInstance().diffuser(obj, "rem");
            }
            tr.commit();
//            em.getTransaction().commit();
        } catch (Exception e) {

            tr.rollback();
            logger.error("Erreur lors de la suppression du beans : " + e);
        }
//        em.getTransaction().begin();
//        em.remove(obj);




//        em.close();
    }

    public List<Object> findall() {
        try {
//            em = dAOFactory.getEntityManager();

//            em.getTransaction().begin();

            Class classasso = this.getClassAssocie();

            String req = "SELECT f FROM " + classasso.getSimpleName() + " f";
            System.out.println("req : " + req);
            Query query = em.createQuery(req);
//            query.setHint("eclipselink.cache-usage", "CheckCacheOnly");



            List<Object> result = query.getResultList();

            return result;
        } catch (SecurityException ex) {
            Logger.getLogger(AbstrDao.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(AbstrDao.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (em != null) {
//                em.close();
//      em.close();
                System.out.println("FINALLYY");
            }
        }
        return null;
    }

    public AbstrDao() {
    }

    public Class getClassAssocie() {
        return classAssocie;
    }

    public void setClassAssocie(Class classAssocie) {
        this.classAssocie = classAssocie;
    }

    public DAOFactory getdAOFactory() {
        return dAOFactory;
    }

    public void setdAOFactory(DAOFactory dAOFactory) {
        this.dAOFactory = dAOFactory;
    }

    public EntityManager getEm() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }
}

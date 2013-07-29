/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityExistsException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.eclipse.persistence.internal.jpa.querydef.BasicCollectionJoinImpl;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import rssagregator.beans.Flux;
import rssagregator.beans.Item;

/**
 *
 * @author clem
 *
 *
 *
 */
public class DaoItem extends AbstrDao {

//    Flux where_clause_flux = null;
    List<Flux> where_clause_Flux;
    String order_by;
    Boolean order_desc;
    Integer fistResult;
    Integer maxResult;
    Date date1;
    Date date2;
    protected org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DaoItem.class);

    protected DaoItem(DAOFactory daof) {

        em = daof.getEntityManager();
        this.dAOFactory = daof;
        this.classAssocie = Item.class;
        where_clause_Flux = new ArrayList<Flux>();
        order_desc = false;
    }
    private static final String REQ_FIND_BY_HASH = "SELECT i FROM Item i where i.hashContenu=:hash";
    private static final String REQ_FIND_BY_HASH_AND_FLUX = "SELECT item FROM Item item JOIN item.listFlux flux where item.hashContenu IN (:hash) AND flux.ID=:fluxid";
//    private static final String REQ_FIND_ALL_AC_LIMIT = "SELECT item FROM Item LIMIT :prem, :nbr";
//    private static final String REQ_FIND_ALL_AC_LIMIT = "SELECT item FROM Item item JOIN item.listFlux flux";
    private static final String REQ_FIND_ALL_AC_LIMIT = "SELECT item FROM Item item ORDER BY item.dateRecup DESC";
    private static final String REQ_COUNT_ALL = "SELECT count(item.ID) FROM Item item";
    private static final String REQ_FIND_HASH = "SELECT item.hashContenu FROM Item item JOIN item.listFlux fl WHERE fl.ID=:idfl ORDER BY item.ID DESC";

    /**
     * *
     * @param hash
     */
    /**
     * *
     * Permet de trouver un item à partir de son hash
     *
     * @param hash
     * @return L'item ou null si pas de réponse
     */
    public synchronized Item findByHash(String hash) {
//        em = dAOFactory.getEntityManager();
//        em.getTransaction().begin();
        Query query = em.createQuery(REQ_FIND_BY_HASH);
        query.setParameter("hash", hash);
        try {

            Item result = (Item) query.getSingleResult();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Item> findCretaria() {

//        em = dAOFactory.getEntityManager();
        int i;

        CriteriaBuilder cb = em.getCriteriaBuilder();
        List<Predicate> listWhere = new ArrayList<Predicate>();

        CriteriaQuery<Item> cq = cb.createQuery(Item.class);
        Root<Item> root = cq.from(Item.class);


        if (where_clause_Flux != null && where_clause_Flux.size() > 0) {
            Join joinFlux = root.join("listFlux");
            listWhere.add(joinFlux.in(where_clause_Flux));
        }

        if (date1 != null && date2 != null) {
            listWhere.add(cb.and(cb.between(root.<Date>get("dateRecup"), date1, date2)));
        }
//        
        // Le ORDER BY
        if (order_by != null) {
            if (order_desc != null && order_desc) {
                System.out.println("DESC");
                cq.orderBy(cb.desc(root.get(order_by)));
            } else {
                System.out.println("ASC");
                cq.orderBy(cb.asc(root.get(order_by)));
            }
        }

        // On applique les wheres
        if (listWhere.size() == 1) {

            cq.where(listWhere.get(0));
        } else if (listWhere.size() > 1) {
            Predicate pr = cb.and(listWhere.get(0));
            for (i = 1; i < listWhere.size(); i++) {
                pr = cb.and(pr, listWhere.get(i));
            }
            cq.where(pr);
        }

        // application de la limite
        TypedQuery<Item> tq = em.createQuery(cq);
        if (fistResult != null && maxResult != null) {
            tq.setMaxResults(maxResult);
            tq.setFirstResult(fistResult);
        } else {
        }
        return tq.getResultList();
    }

    public static void main(String[] args) {

        DaoItem dao = DAOFactory.getInstance().getDaoItem();

        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy");
        DateTime dateTime = fmt.parseDateTime("01/01/2014");
        Date date1 = dateTime.toDate();

        dateTime = fmt.parseDateTime("01/01/2015");
        Date date2 = dateTime.toDate();

        dao.setDate1(date1);
        dao.setDate2(date2);

        List<Item> resu = dao.findCretaria();
    }

    /**
     * *
     * Retourne le nombre total d'item dans la base de données. Si une jointure
     * est demandé (voir les where clause criteria de cette dao), le count sera
     * restreint aux items joins au flux
     *
     * @return
     */
    public Integer findNbMax() {

//        em = DAOFactory.getInstance().getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        List<Predicate> listWhere = new ArrayList<Predicate>();

        CriteriaQuery cq = cb.createQuery(Item.class);
        Root root = cq.from(Item.class);

        if (where_clause_Flux != null && where_clause_Flux.size() > 0) {
            Join joinFlux = root.join("listFlux");
            listWhere.add(joinFlux.in(where_clause_Flux));
        }

        if (date1 != null && date2 != null) {
            listWhere.add(cb.and(cb.between(root.<Date>get("dateRecup"), date1, date2)));
        }

        // On applique les wheres
        int i;
        if (listWhere.size() == 1) {
            cq.where(listWhere.get(0));
        } else if (listWhere.size() > 1) {
            Predicate pr = cb.and(listWhere.get(0));
            for (i = 1; i < listWhere.size(); i++) {
                pr = cb.and(pr, listWhere.get(i));
            }
            cq.where(pr);
        }

        cq.select(cb.count(root));
        TypedQuery<Item> tq = em.createQuery(cq);

        List resu = tq.getResultList();

        try {
            Integer retour = new Integer(resu.get(0).toString());
            return retour;
        } catch (Exception e) {
            System.out.println("ERRRRR");
            return null;
        }
    }

    public List<Item> findAllLimit(Long premier, Long nombre) {

        Query query = em.createQuery(REQ_FIND_ALL_AC_LIMIT);
        query.setFirstResult(premier.intValue());
        query.setMaxResults(nombre.intValue());


        List<Item> listResult = query.getResultList();
//        em.close();
        return listResult;
    }

    /**
     * *
     * Trouve les items possédant un hash présent dans la liste de hash envoyé
     * en paramètre tout en étant lié au flux précisé en paramètre
     *
     * @param hashContenu : List des items. On va utiliser leur hash pou
     * effectuer la recerche.
     * @param flux : Flux devant être lié aux items
     * @return List de flux possédant un hash dans la liste et étant lié au flux
     * sélectioné.
     */
    public synchronized List<Item> findHashFlux(List<Item> hashContenu, Flux flux) {
//        em = dAOFactory.getEntityManager();
//        em.getTransaction().begin();

        // Constuction de la liste des hash
        int i;
        String hashParamSQL = "";
        for (i = 0; i < hashContenu.size(); i++) {
            hashParamSQL += "'" + hashContenu.get(i).getHashContenu() + "', ";
        }
        if (hashParamSQL.length() > 2) {
            hashParamSQL = hashParamSQL.substring(0, hashParamSQL.length() - 2);
        }
// TODO : C'est laid de faire des requete mon préparée en plein milieu du code. Mais on n'arive pas a préparer une requete basée su une liste de string
//        Query query = em.createQuery("SELECT item FROM Item item JOIN item.listFlux flux where item.hashContenu IN ("+hashParamSQL+") AND flux.ID=:fluxid");
        Query query = em.createQuery("SELECT item FROM Item item LEFT JOIN fetch item.listFlux WHERE item.hashContenu IN (" + hashParamSQL + ")");
        //LEFT JOIN FETCH item.listFlux

        List<Item> resuList;
        resuList = query.getResultList();
        return resuList;
    }

    /**
     * *
     * retourne l'item possédant le hash
     *
     * @param hash
     * @return
     */
    public Item findItemByHash(String hash) {

        Item item;

        Query query = em.createQuery("SELECT i FROM Item i WHERE i.hashContenu=:hash");
        query.setParameter("hash", hash);
        item = (Item) query.getSingleResult();
        return item;
    }

    /**
     * *
     * Cette méthode est utilisée au démarrage de l'application pour précharger
     * les derniers hash des flux.
     *
     * @param fl
     * @param i
     */
    public List<String> findLastHash(Flux fl, int i) {

//        em = dAOFactory.getEntityManager();
        Query query = em.createQuery(REQ_FIND_HASH);
        query.setParameter("idfl", fl.getID());
//        query.setParameter("lim", i);
        query.setFirstResult(0);
        query.setMaxResults(i);

        List<String> resu = query.getResultList();

        return resu;
//        int j;
//         
//        for (j=0; j<resu.size(); j++){
//            System.out.println("hash depart : " + resu.get(j));
//            
//        }        

    }

    public String getOrder_by() {
        return order_by;
    }

    public void setOrder_by(String order_by) {
        this.order_by = order_by;
    }

    public Boolean getOrder_desc() {
        return order_desc;
    }

    public void setOrder_desc(Boolean order_desc) {
        this.order_desc = order_desc;
    }

    public Integer getFistResult() {
        return fistResult;
    }

    public void setFistResult(Integer fistResult) {
        this.fistResult = fistResult;
    }

    public Integer getMaxResult() {
        return maxResult;
    }

    public void setMaxResult(Integer maxResult) {
        this.maxResult = maxResult;
    }

    public Date getDate1() {
        return date1;
    }

    public void setDate1(Date date1) {
        this.date1 = date1;
    }

    public Date getDate2() {
        return date2;
    }

    public void setDate2(Date date2) {
        this.date2 = date2;
    }

    public List<Flux> getWhere_clause_Flux() {
        return where_clause_Flux;
    }

    public void setWhere_clause_Flux(List<Flux> where_clause_Flux) {
        this.where_clause_Flux = where_clause_Flux;
    }

    public synchronized void enregistrement(Item item, Flux flux) {

        Boolean err = false;

        if (item.getID() != null) {
            err = true;
        }

        // Si l'item est nouvelle (elle n'a pas d'id)
        if (!err) {
            try {
                em.getTransaction().begin();
                em.persist(item);
                em.getTransaction().commit();
                flux.getLastEmpruntes().add(0, item.getHashContenu());
            } catch (EntityExistsException existexeption) { // En cas d'erreur, on se rend compte qu'une item possédant le hash existe déjà
                err = true;
            } catch (RollbackException e) {
                err = true;
            } catch (Exception e) {
                System.out.println("ERR");
            }
        }

        if (err) {
            Item it = findByHash(item.getHashContenu());
            it.getListFlux().add(flux);
            logger.debug("Item déjà existente lors de l'enregistrement");
            try {
                em.getTransaction().begin();
                em.merge(it);
                em.getTransaction().commit();
                flux.getLastEmpruntes().add(0, item.getHashContenu());
            } catch (Exception e) {
                logger.debug("ERREURR");
            }
        }
    }

    /**
     * *
     * Retrouve la liste des items appartenant au flux
     *
     * @param idflux : id du flux
     * @return liste d'item
     */
    List<Item> findByFlux(Long idflux) {
        String REQ = "SELECT item FROM Item item JOIN item.listFlux flux where flux.ID=:fluxid";
        List<Item> item;
        Query query = em.createQuery(REQ);
        query.setParameter("fluxid", idflux);
        item = (List<Item>) query.getResultList();
        System.out.println("LISTTTT : " + item.size());
        return item;

    }

    /**
     * *
     * Met les paramettre de critère à null, utile car la daoItem est singleton,
     * cette commande permet donc de réinitialiser les paramettres de recherche.
     */
    public void initcriteria() {
//        where_clause_flux = null;
        
        where_clause_Flux = new ArrayList<Flux>();
        order_by = null;
        order_desc = null;
        fistResult = null;
        maxResult = null;
        date1 = null;
        date2 = null;
    }
}

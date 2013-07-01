/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rssagregator.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import rssagregator.beans.ServeurSlave;
import rssagregator.beans.form.DAOGenerique;
import rssagregator.beans.form.ServeurSlaveForm;
import rssagregator.dao.DAOFactory;

/**
 *
 * @author clem
 */
@WebServlet(name = "slave", urlPatterns = {"/slave"})
public class ServeurSlaveSrlvt extends HttpServlet {

    public static final String VUE = "/WEB-INF/serveurslavejsp.jsp";
    public static final String ATT_FORM = "form";
    public static final String ATT_BEAN = "obj";
    public static final String ATT_LIST_OBJ = "list";

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {


        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        request.setAttribute("navmenu", "config");
        Map<String, String> redirmap = null;
        ServeurSlave obj = null;



        // récupération de l'action
        String action = request.getParameter("action");
        if (action == null) {
            action = "list";
        }
        request.setAttribute("action", action);

        DAOGenerique dao = DAOFactory.getInstance().getDAOGenerique();
        dao.setClassAssocie(ServeurSlave.class);

        ServeurSlaveForm form = new ServeurSlaveForm();

        if (action.equals("mod") || action.equals("rem")) {
            try {
                obj = (ServeurSlave) dao.find(new Long(request.getParameter("id")));
                request.setAttribute(ATT_BEAN, obj);
                System.out.println("ICI");
            } catch (Exception e) {
                System.out.println("EXEPTION" + e);
            }
        }

        // Le bind
        if (request.getMethod().equals("POST")) {
            obj = (ServeurSlave) form.bind(request, obj, ServeurSlave.class);
        }

        if (action.equals("list")) {
            List<Object> list = dao.findall();
            request.setAttribute(ATT_LIST_OBJ, list);
            System.out.println("NBR : " + list.size());
        }

        if (form.getValide()) {
            if (action.equals("add")) {
                try {
                    dao.creer(obj);
                    redirmap = new HashMap<String, String>();
                    redirmap.put("url", "slave");
                    redirmap.put("msg", "youpi");
                    request.setAttribute("redirmap", redirmap);
                } catch (Exception ex) {
                    Logger.getLogger(ServeurSlaveSrlvt.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (action.equals("mod")) {
                try {
                    dao.modifier(obj);

                    redirmap = new HashMap<String, String>();
                    redirmap.put("url", "slave");
                    redirmap.put("msg", "Modification effectuées");
                    request.setAttribute("redirmap", redirmap);

                } catch (Exception ex) {
                    Logger.getLogger(ServeurSlaveSrlvt.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
        if (action.equals("rem")) {
            try {
                dao.remove(obj);
                redirmap = new HashMap<String, String>();
                redirmap.put("url", "slave");
                redirmap.put("msg", "Suppression effectuées");
                request.setAttribute("redirmap", redirmap);
                System.out.println("SUPPPRESSION");
            } catch (Exception ex) {
                Logger.getLogger(ServeurSlaveSrlvt.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        this.getServletContext().getRequestDispatcher(VUE).forward(request, response);
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
package generate;

import hibernate.HibernateConnector;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import domain.AppColumn;
import domain.AppTable;
import domain.Application;
import domain.Rule;

public class GenerateController {
	private Parser parser;
	private Writer writer;
	private Session hibernateSession;

	public GenerateController() {
		parser = new Parser();
		SessionFactory factory = new HibernateConnector().getSessionFactory();
		hibernateSession = factory.openSession();

	}

	public ArrayList<String> generate(int idApp) {
		Transaction tx = null;
		List<Rule> rules = new ArrayList<Rule>();
		ArrayList<String> list = new ArrayList<String>();

		try {
			tx = hibernateSession.beginTransaction();
			@SuppressWarnings(value = { "unchecked" })
			List<AppTable> tables = hibernateSession.createQuery(
					"FROM AppTable WHERE id = :appid")
					.setParameter("appid", idApp)
					.list();
			for (AppTable table : tables) {
				int tableId = table.getId();
				System.out.println("Table: " + table.getName());
				@SuppressWarnings(value = { "unchecked" })
				List<AppColumn> cols = hibernateSession
						.createQuery("FROM AppColumn WHERE table = :tableid")
						.setParameter("tableid", table).list();
				for (AppColumn col : cols) {
					System.out.println("Column: " + col.getName() + "(id:"
							+ col.getId() + ")");
					Query query = hibernateSession
							.createSQLQuery("SELECT RULE_ID FROM RULECOLUMNS WHERE COLUMN_ID = :colid");
					List ruleIdList = query.setParameter("colid", col.getId())
							.list();
					for (Object id : ruleIdList) {
						Rule foundRule = new Rule();
						hibernateSession.load(foundRule,
								Integer.parseInt(id.toString()));
						if (foundRule.isToBeGenerated() == true) {
							rules.add(foundRule);
						}
					}
				}

			}

			System.out.println(rules.size());
			for (Rule rule : rules) {
				System.out.println(rule.getErrorMessage());
			}

			for (Rule r : rules) {
				if (r.isToBeGenerated()) {
					String generatedCode = parser.generateCode(r);
					r.setGeneratedCode(generatedCode);
					hibernateSession.save(r);
					list.add(generatedCode);
				}
			}
			tx.commit();

		} catch (HibernateException e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
		} finally {
			hibernateSession.close();
		}
		return list;
	}

	public Parser getParser() {
		return parser;
	}

	public void setParser(Parser parser) {
		this.parser = parser;
	}

	public Writer getWriter(String a) {
		if (a.equals("mysql")) {
			writer = new MySQLWriter();
		} else if (a.equals("oracle")) {
			writer = new OracleWriter();
		}
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}

}
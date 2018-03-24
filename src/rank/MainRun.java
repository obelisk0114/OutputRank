package rank;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.io.Writer;

import java.util.Comparator;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainRun {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String query = "tree";
		String inputData = "Inverted_index_Sample";
		String totalNumber = "";
		try {
			switch (args.length) {
			case 0:
				break;
			case 1:
				inputData = args[0];
				break;
			case 2:
				query = args[0];
				inputData = args[1];
				break;
			case 3:
				query = args[0];
				inputData = args[1];
				totalNumber = args[2];
				break;
			default:
				System.out.println("\n    Invalid input parameters!"
					+ "\n    Usage: java -jar rank.jar         # data = Inverted_index_Sample, query = tree"
					+ "\n    Usage: java -jar rank.jar <s>     # data = s, query = tree"
					+ "\n    Usage: java -jar rank.jar <s> <q> # data = s, query = q");
				System.exit(1);
			}
			
			FileReader fr = new FileReader(inputData);
			BufferedReader br = new BufferedReader(fr);
			
			String line;
			List<List<String>> term = new ArrayList<List<String>>();
			Map<String, Integer> termSet = new HashMap<String, Integer>();
			while ((line = br.readLine()) != null) {
				String[] termSeparate = line.split("\t");
				int index = termSet.getOrDefault(termSeparate[0], termSet.size());
				if (index == termSet.size()) {
					List<String> appear = new ArrayList<String>();
					term.add(appear);
					termSet.put(termSeparate[0], index);
				}
				
				String[] postCount = termSeparate[1].split(";");
				for (String eachPost : postCount) {
					term.get(index).add(eachPost);
				}
			}
			br.close();
			
			File file = new File("ranking.json");
			Writer writer = new BufferedWriter(new OutputStreamWriter(
				    new FileOutputStream(file), "UTF-8"));
			JSONArray allLinks = new JSONArray();
			String[] queryTerm = query.split(" ");
			
			HashMap<String, Double> map = new HashMap<String, Double>();
			ValueComparator bvc = new ValueComparator(map);
			TreeMap<String, Double> documentScore = new TreeMap<String, Double>(bvc);
			
			int[] pos = new int[queryTerm.length];
			for (int i = 0; i < pos.length; i++) {
				pos[i] = termSet.get(queryTerm[i]);				
			}
			termSet = null;
			
			// Used to record document length
			Map<String, Integer> doc = new HashMap<String, Integer>();
			
			// Find the documents that all the terms appeared
			for (int singleTerm : pos) {
				List<String> single = term.get(singleTerm);
				for (String pairs : single) {
					String[] tulip = pairs.split(":");
					int val = doc.getOrDefault(tulip[0], 0);
					doc.put(tulip[0], val + 1);
				}
			}
			Iterator<Map.Entry<String, Integer>> entries = doc.entrySet().iterator();
			while (entries.hasNext()) {
				Map.Entry<String, Integer> entry = entries.next();
				if (entry.getValue() != pos.length) {
					entries.remove();
				}
			}
			
			// Reset map
			for (String key : doc.keySet()) {
				doc.put(key, 0);
			}
			
			double c = 0;                    // total number of words in collections
			double avgdl = 1;                // average length of documents
			double N = 1;                    // total number of documents
			int qf = 1;                      // query frequency of term 
			double k1 = 1.2;
			double b = 0.75;
			double k2 = 100;
			
			if (!totalNumber.equals("")) {
				FileReader fr2 = new FileReader(totalNumber);
				BufferedReader br2 = new BufferedReader(fr2);	
				
				String countTerm = "";
				int countAll = 0;
				while ((countTerm = br2.readLine()) != null) {
					String[] countSeparate = countTerm.split("\t");
					if (doc.containsKey(countSeparate[0])) {
						doc.put(countSeparate[0], Integer.parseInt(countSeparate[1]));
					}
					countAll++;
					c += Integer.parseInt(countSeparate[1]);
				}
				avgdl = c / countAll;
				N = countAll;
				br2.close();
			}
			else {
				Set<String> set = new HashSet<String>();
				for (List<String> eachTerm : term) {
					for (String eachOne : eachTerm) {
						String[] travelDocument = eachOne.split(":");
						if (doc.containsKey(travelDocument[0])) {
							int value = Integer.parseInt(travelDocument[1]) + 
									doc.get(travelDocument[0]);
							doc.put(travelDocument[0], value);
						}
						set.add(travelDocument[0]);
						c += Integer.parseInt(travelDocument[1]);
					}
				}
				avgdl = c / set.size();
				N = set.size();
			}
			
			for (Map.Entry<String, Integer> entry : doc.entrySet()) {
				for (int j = 0; j < pos.length; j++) {
					double K = k1 * ((1 - b) + b * entry.getValue() / avgdl);
					int ni = term.get(pos[j]).size();
					int fi = 0;
					for (String s : term.get(pos[j])) {
						String[] separate = s.split(":");
						if (separate[0].equals(entry.getKey())) {
							fi = Integer.parseInt(separate[1]);
							break;
						}
					}
					
					double bm25 = Math.log(1 / ((ni + 0.5) / (N - ni + 0.5))) * 
							((k1 + 1) * fi / (K + fi)) * 
							((k2 + 1) * qf / (k2 + qf));
					double sum = map.getOrDefault(entry.getKey(), 0.) + bm25;
					map.put(entry.getKey(), sum);
				}
				
			}
			
			documentScore.putAll(map);
			map = null;
			System.out.println("query = " + query);
			System.out.println(documentScore);
			
			for (Map.Entry<String, Double> entry : documentScore.entrySet()) {
				JSONObject post = new JSONObject();
				post.put(entry.getKey(), entry.getValue());
				allLinks.put(post);
			}
			writer.write(allLinks.toString());
			writer.close();
			
		} catch (IOException ie) {
			ie.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}

class ValueComparator implements Comparator<String> {
    Map<String, Double> base;

    public ValueComparator(Map<String, Double> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with
    // equals.
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}

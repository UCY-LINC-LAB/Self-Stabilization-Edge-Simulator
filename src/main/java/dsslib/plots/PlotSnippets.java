package dsslib.plots;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class PlotSnippets {

    public static String importLibraries(){
       return String.join("\n"
                ,"import numpy as np"
               ,"import pandas as pd"
               ,"import io"
               ,"import base64"
               ,"import matplotlib.pyplot as plt"
               ,"import seaborn as sns");
    }

    public static String convertGraphDef(){
        return String.join("\n"
                ,"def convert_graph(f):"
                ,"  img = io.BytesIO()"
                ,"  f.savefig(img, format='png')"
                ,"  img.seek(0)"
                ,"  graph_url = base64.b64encode(img.getvalue()).decode()"
                ,"  return 'data:image/png;base64,{}'.format(graph_url)");
    }

    public static String plotLineExample(){
        return String.join("\n",
                "def plot_line():"
                ,"  df = pd.DataFrame(dict(time=np.arange(500), value=np.random.randn(500).cumsum()))"
                ,"  g = sns.relplot(x='time', y='value', kind='line', data=df)"
                ,"  g.fig.autofmt_xdate()"
                ,"  return g.fig"
                );
    }

    public static String getPath(String file){
        //String path = PlotSnippets.class.getResource("/"+file).getPath();
        try {
            return writeResourceToFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String writeResourceToFile(String file) throws IOException {
        String base = "plots/";
        ClassLoader loader = PlotSnippets.class.getClassLoader();
        InputStream configStream = loader.getResourceAsStream(base+file);
        File dir = new File("/tmp/ssedge/"+base);
        dir.mkdirs();
        File tmpFile = Paths.get(dir.getAbsolutePath(),file).toFile();
        Files.copy(configStream, tmpFile.toPath(), REPLACE_EXISTING);
        return tmpFile.getAbsolutePath();
    }
    public static byte[] readFile(String file) throws IOException {
        String path = PlotSnippets.class.getResource("/"+file).getPath();
        return Files.readAllBytes(Paths.get(path));
    }
}

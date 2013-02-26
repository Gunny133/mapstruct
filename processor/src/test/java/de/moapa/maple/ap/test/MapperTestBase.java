/**
 *  Copyright 2012-2013 Gunnar Morling (http://www.gunnarmorling.de/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.moapa.maple.ap.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import de.moapa.maple.ap.MappingProcessor;
import org.testng.annotations.BeforeClass;

public abstract class MapperTestBase {

	private JavaCompiler compiler;
	private String sourceDir;
	private String classOutputDir;
	private String sourceOutputDir;
	private List<File> classPath;
	private List<String> libraries;

	public MapperTestBase(String... libraries) {
		this.libraries = Arrays.asList( libraries );
	}

	@BeforeClass
	public void setup() throws Exception {

		compiler = ToolProvider.getSystemJavaCompiler();

		String basePath = getBasePath();

		sourceDir = basePath + "/src/test/java";
		classOutputDir = basePath + "/target/compilation-tests/classes";
		sourceOutputDir = basePath + "/target/compilation-tests/generated-sources/mapping";

		String testDependenciesDir = basePath + "/target/test-dependencies/";

		classPath = new ArrayList<File>();
		for ( String oneLibrary : libraries ) {
			classPath.add( new File( testDependenciesDir, oneLibrary ) );
		}

		createOutputDirs();

		Thread.currentThread().setContextClassLoader(
				new URLClassLoader(
						new URL[] { new File( classOutputDir ).toURI().toURL() },
						Thread.currentThread().getContextClassLoader()
				)
		);
	}

	protected File[] getSourceFiles(Class<?>... clazz) {

		File[] sourceFiles = new File[clazz.length];

		for ( int i = 0; i < clazz.length; i++ ) {

			sourceFiles[i] = new File(
					sourceDir +
							File.separator +
							clazz[i].getName().replace( ".", File.separator ) +
							".java"
			);
		}


		return sourceFiles;
	}

	protected boolean compile(DiagnosticCollector<JavaFileObject> diagnostics, File... sourceFiles) {

		StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );

		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects( sourceFiles );

		try {
			fileManager.setLocation( StandardLocation.CLASS_PATH, classPath );
			fileManager.setLocation( StandardLocation.CLASS_OUTPUT, Arrays.asList( new File( classOutputDir ) ) );
			fileManager.setLocation( StandardLocation.SOURCE_OUTPUT, Arrays.asList( new File( sourceOutputDir ) ) );
		}
		catch ( IOException e ) {
			throw new RuntimeException( e );
		}

		CompilationTask task = compiler.getTask(
				null,
				fileManager,
				diagnostics,
				Collections.<String>emptyList(),
				null,
				compilationUnits
		);
		task.setProcessors( Arrays.asList( new MappingProcessor() ) );

		return task.call();
	}

	private String getBasePath() {

		try {
			return new File( "." ).getCanonicalPath();
		}
		catch ( IOException e ) {
			throw new RuntimeException( e );
		}
	}

	private void createOutputDirs() {

		File directory = new File( classOutputDir );
		deleteDirectory( directory );
		directory.mkdirs();

		directory = new File( sourceOutputDir );
		deleteDirectory( directory );
		directory.mkdirs();
	}

	private void deleteDirectory(File path) {
		if ( path.exists() ) {
			File[] files = path.listFiles();
			for ( int i = 0; i < files.length; i++ ) {
				if ( files[i].isDirectory() ) {
					deleteDirectory( files[i] );
				}
				else {
					files[i].delete();
				}
			}
		}
		path.delete();
	}

}
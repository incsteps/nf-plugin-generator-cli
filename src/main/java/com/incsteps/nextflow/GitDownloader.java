package com.incsteps.nextflow;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GitDownloader {

    private final Logger logger = LoggerFactory.getLogger(GitDownloader.class);

    public File downloadRepository(String repositorioUrl) throws IOException, GitAPIException {
        File directorioTemporal = Files.createTempDirectory("nf-plugin-generator-cli").toFile();

        try {
            logger.debug("Clonando el repositorio desde: {}", repositorioUrl);
            Git.cloneRepository()
                    .setURI(repositorioUrl)
                    .setDirectory(directorioTemporal)
                    .call();
            logger.debug("Repositorio clonado exitosamente en: {}", directorioTemporal.getAbsolutePath());
            return directorioTemporal;
        } catch (GitAPIException e) {
            removeRepository(directorioTemporal);
            throw e;
        }
    }

    private void removeRepository(File directorio) {
        if (directorio.isDirectory()) {
            File[] archivos = directorio.listFiles();
            if (archivos != null) {
                for (File archivo : archivos) {
                    removeRepository(archivo);
                }
            }
        }
        if (directorio.delete()) {
            logger.info("Directorio temporal eliminado: {}", directorio.getAbsolutePath());
        } else {
            logger.error("No se pudo eliminar el directorio temporal: {}", directorio.getAbsolutePath());
        }
    }

    public File pullRepo(String repositorioGit, String branch) {
        try {
            var ret = downloadRepository(repositorioGit);
            return ret;
        } catch (IOException e) {
            logger.error("Error al crear el directorio temporal: {}", e.getMessage());
            return null;
        } catch (GitAPIException e) {
            logger.error("Error al clonar el repositorio: {}", e.getMessage());
            return null;
        }
    }
}
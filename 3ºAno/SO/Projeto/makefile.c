#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

int main() {
  int cl_sv = 0, sv_cl = 0;

  cl_sv = mkfifo("cl_sv", 0777);
  if (cl_sv < 0) {
    perror("erro em mkfifo(cl_sv)");
    exit(-1);
  }
  sv_cl = mkfifo("sv_cl", 0777);
  if (sv_cl < 0) {
    perror("erro em mkfifo(sv_cl)");
    exit(-1);
  }
  return 0;
}

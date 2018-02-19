from subprocess import run
from subprocess import PIPE
from os.path import basename

# Path to the compiler jar
pika_compiler='C:/Users/HP/OneDrive/cmpt379/pika/qa/pika.jar'

# Path to the emulator
asm_emulator = 'C:/Users/HP/OneDrive/cmpt379/pika/ASM_Emulator/ASMEmu.exe'

# output directory
out_dir = './temp/'

# temporary filename
temp_pika_file_name = 'temp.pika'

if not out_dir.endswith('/'):
	out_dir += '/'

combined_temp_pika_path = out_dir + temp_pika_file_name

def __run(cmd_args):
	p = run(cmd_args, stdout=PIPE, stderr=PIPE)
	return (p.stdout.decode('unicode_escape').replace('\r\n', '\n'),
			p.stderr.decode('unicode_escape').replace('\r\n', '\n'))

def asm_output_file(asm_input_file_name):
	p = __run([asm_emulator, asm_input_file_name, out_dir])
	assert len(p[1]) == 0
	return p[0]

def compile_file(pika_input_file_name, error=False):
	stderr = __run(['java','-jar', pika_compiler, pika_input_file_name, out_dir])[1]
	if error:
		assert len(stderr) > 0
		return stderr
	else:
		assert len(stderr) == 0
		return out_dir + basename(pika_input_file_name).replace('.pika', '.asm')

def compile_error_file(pika_input_file_name):
	return compile_file(pika_input_file_name, error=True)

def compile_run_file(pika_input_file_name):
	asm_input_file_name = compile_file(pika_input_file_name)
	return asm_output_file(asm_input_file_name)

def temp_pika(pika_code):
	f = open(combined_temp_pika_path, 'w')
	f.write(pika_code)
	f.close()

def compile_string(pika_code, error=False):
	temp_pika(pika_code)
	return compile_file(combined_temp_pika_path, error=error)

def compile_error_string(pika_code):
	return compile_string(pika_code, error=True)

def compile_run_string(pika_code):
	temp_pika(pika_code)
	return compile_run_file(combined_temp_pika_path)
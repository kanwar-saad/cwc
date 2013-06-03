chunk_size = 1024


def get_next_chunk(src, size):
    data = src.read(size)
    return data 


src_file = open('image.jpg', 'rb')
dst_file = open('new.jpg', 'wb')

chunk = ""
iterations = 0
while True:
    iterations += 1
    chunk = get_next_chunk(src_file, chunk_size)
    dst_file.write(chunk) 
    if (len(chunk) < chunk_size):
        break

src_file.close()
dst_file.close()

print "Iterations = ", str(iterations)

